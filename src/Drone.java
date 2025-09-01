import java.awt.*;
import java.util.List;

class Drone extends Enemy {
    double patrolSpeed = 1.8;
    int sightRange = 160, attackRange = 40;
    int windupTime = 8, windup = 0;
    double baseY;

    Drone(int x, int y, int w, int h){
        super(x,y,w,h);
        gravity = 0; damageOnHit = 18; health = 60; baseY = y; vx = patrolSpeed;
    }

    @Override
    void update(List<Rect> solids, Player player, int tick) {
        y = baseY + Math.sin(tick*0.05) * 10;

        double dx = (player.x + player.w/2) - (x + w/2);
        double adx = Math.abs(dx);
        dir = dx>0 ? 1 : -1;

        double targetSpeed = (adx < sightRange) ? patrolSpeed*1.4 : patrolSpeed;
        vx = dir * targetSpeed;
        x += vx;

        if (adx < attackRange) { if (attackCooldown==0 && windup==0) windup = windupTime; }
        if (windup>0){ windup--; if (windup==0){ attackWindow=8; attackCooldown=28; } }
        if (attackCooldown>0) attackCooldown--;
        if (attackWindow>0)   attackWindow--;
    }

    @Override Rect attackHitbox() { return new Rect((int)x, (int)y, w, h); }

    @Override void draw(Graphics2D g2, int tick) {
        Art.drawDrone(g2, bounds(), dir, isAttacking(tick), health, 60, tick);
    }
}
