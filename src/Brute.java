import java.awt.*;
import java.util.List;

class Brute extends Enemy {
    double patrolSpeed = 1.0, chaseSpeed = 1.5;
    int sightRange = 120, attackRange = 42;
    int windupTime = 18, windup = 0;

    final double homeX;

    Brute(int x, int y, int w, int h){
        super(x,y,w,h);
        homeX = x; vx = patrolSpeed; damageOnHit = 35; health = 100;
    }

    @Override
    void update(List<Rect> solids, Player player, int tick) {
        vy += gravity;

        double dx = (player.x + player.w/2) - (x + w/2);
        double adx = Math.abs(dx);
        boolean sameHeight = Math.abs(player.y - y) < 48;

        double targetSpeed = (sameHeight && adx < sightRange) ? chaseSpeed : patrolSpeed;
        if (sameHeight && adx < sightRange) dir = dx>0?1:-1;
        if (Math.abs((x + w/2) - homeX) > 160) dir = ((x + w/2) > homeX) ? -1 : 1;
        vx = dir * targetSpeed;

        if (onGround) {
            Rect probe = new Rect((int)(x + (dir>0 ? w + 2 : -2)), (int)(y + h + 1), 3, 3);
            boolean groundAhead = false;
            for (Rect s : solids){ if (probe.intersects(s)) { groundAhead=true; break; } }
            if (!groundAhead){ dir=-dir; vx = dir*targetSpeed; }
        }

        x += vx;
        Rect b = bounds();
        for (Rect s : solids){
            if (b.intersects(s)){
                if (vx>0) x = s.x - w; else x = s.x + s.w;
                dir = -dir; vx = 0; b = bounds();
            }
        }

        y += vy; b = bounds(); onGround = false;
        for (Rect s : solids){
            if (b.intersects(s)){
                if (vy>0){ y = s.y - h; vy=0; onGround=true; }
                else if (vy<0){ y = s.y + s.h; vy=0; }
                b = bounds();
            }
        }

        if (sameHeight && adx < attackRange) {
            if (attackCooldown==0 && windup==0) windup = windupTime;
        }
        if (windup>0){ windup--; if (windup==0){ attackWindow=12; attackCooldown=50; } }
        if (attackCooldown>0) attackCooldown--;
        if (attackWindow>0)   attackWindow--;
    }

    @Override Rect attackHitbox() {
        int aw = 30, ah = 20;
        int ax = (int)(x + (dir>0? w-2 : -aw));
        int ay = (int)(y + h - 26);
        return new Rect(ax, ay, aw, ah);
    }

    @Override void draw(Graphics2D g2, int tick) {
        Art.drawBrute(g2, bounds(), dir, isAttacking(tick), health, 100, tick);
    }
}
