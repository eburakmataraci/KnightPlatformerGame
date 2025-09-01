import java.awt.*;
import java.awt.AlphaComposite;
import java.util.List;

class Swordsman extends Enemy {
    double patrolSpeed=1.4, chaseSpeed=2.0;
    int sightRange=120, attackRange=46;
    int windupTime=14, windup=0;
    final double homeX;

    Swordsman(int x,int y,int w,int h){ super(x,y,w,h); homeX=x; vx=patrolSpeed; damageOnHit=22; }
    Swordsman(int x,int y,int w,int h, java.awt.image.BufferedImage sprite){
        super(x,y,w,h,sprite); homeX=x; vx=patrolSpeed; damageOnHit=22;
    }

    @Override
    void update(List<Rect> solids, Player player, int tick) {
        vy+=gravity;
        double dx = (player.x+player.w/2)-(x+w/2);
        double adx=Math.abs(dx);
        boolean sameHeight=Math.abs(player.y-y)<40;

        double targetSpeed=(sameHeight&&adx<sightRange)?chaseSpeed:patrolSpeed;
        if (sameHeight&&adx<sightRange) dir=dx>0?1:-1;
        if (Math.abs((x+w/2)-homeX)>160) dir=((x+w/2)>homeX)?-1:1;
        vx=dir*targetSpeed;

        if(onGround){
            Rect probe=new Rect((int)(x+(dir>0?w+2:-2)),(int)(y+h+1),3,3);
            boolean groundAhead=false;
            for(Rect s:solids){ if(probe.intersects(s)){groundAhead=true;break;} }
            if(!groundAhead){ dir=-dir; vx=dir*targetSpeed; }
        }

        x+=vx; Rect b=bounds();
        for(Rect s:solids){ if(b.intersects(s)){ if(vx>0)x=s.x-w; else x=s.x+s.w; dir=-dir; vx=0; b=bounds(); } }

        y+=vy; b=bounds(); onGround=false;
        for(Rect s:solids){ if(b.intersects(s)){ if(vy>0){y=s.y-h; vy=0; onGround=true;} else if(vy<0){y=s.y+s.h; vy=0;} b=bounds(); } }

        if(sameHeight&&adx<attackRange){ if(attackCooldown==0&&windup==0) windup=windupTime; }
        if(windup>0){ windup--; if(windup==0){ attackWindow=10; attackCooldown=40; } }
        if(attackCooldown>0) attackCooldown--;
        if(attackWindow>0) attackWindow--;
    }

    @Override Rect attackHitbox(){ return new Rect((int)(x+(dir>0?w-2:-26)),(int)(y+h-28),26,18); }

    @Override void draw(Graphics2D g2,int tick){
        if(attackWindow>0){
            Graphics2D gl=(Graphics2D)g2.create();
            gl.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.15f));
            Rect b=bounds();
            gl.setColor(Color.YELLOW);
            gl.fillRect(b.x-2,b.y-2,b.w+4,b.h+4);
            gl.dispose();
        }
        Art.drawEnemyWithSprite(g2,bounds(),dir,isAttacking(tick),health,70,tick,this.sprite);
    }
}
