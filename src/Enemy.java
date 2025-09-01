import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

abstract class Enemy {
    double x,y,vx=0,vy=0;
    int w,h,dir=1;
    boolean alive=true, onGround=false;

    int health=70;
    int attackCooldown=0;
    int attackWindow=0;
    int damageOnHit=10;

    double gravity=0.75;
    BufferedImage sprite = null;

    Enemy(int x,int y,int w,int h){ this(x,y,w,h,null); }

    Enemy(int x,int y,int w,int h,BufferedImage sprite){
        this.x=x; this.y=y; this.w=w; this.h=h; this.sprite=sprite;
    }

    Rect bounds(){ return new Rect((int)Math.round(x), (int)Math.round(y), w,h); }
    void takeDamage(int dmg){ health -= dmg; if (health<=0) alive=false; }
    boolean isAttacking(int tick){ return attackWindow>0; }

    abstract void draw(Graphics2D g2,int tick);
    abstract Rect attackHitbox();
    abstract void update(List<Rect> solids, Player player, int tick);
}
