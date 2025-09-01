import java.awt.*;

class Player {
    double x, y, vx = 0, vy = 0;
    int w = 36, h = 48;

    double speed = 4.6;
    double gravity = 0.75;
    double jumpStrength = 14.8;
    boolean onGround = false;
    int dir = 1;

    int maxHealth = 100, health = 100;
    int lives = 3;
    int invulnTimer = 0;

    int coyote = 0;
    int jumpBuffer = 0;

    boolean attackingLight = false, attackingHeavy = false, rolling = false;
    int lightTimer = 0, heavyTimer = 0, rollTimer = 0;
    int lightCD = 0, heavyCD = 0, rollCD = 0;

    final int LIGHT_WINDOW = 10, HEAVY_WINDOW = 16, ROLL_WINDOW = 12;
    final int LIGHT_COOLDOWN = 18, HEAVY_COOLDOWN = 30, ROLL_COOLDOWN = 36;
    final int LIGHT_DAMAGE = 28, HEAVY_DAMAGE = 50;
    final int ROLL_IFRAMES = 12;
    final double rollSpeed = 6.0;

    Point checkpoint = new Point(0,0);

    Player(int x, int y) { this.x = x; this.y = y; }

    Rect bounds(){ return new Rect((int)Math.round(x), (int)Math.round(y), w, h); }

    Rect lightHitbox(){
        int aw = 28, ah = 20;
        int ax = (int)(x + (dir>0 ? w-2 : -aw));
        int ay = (int)(y + h - 34);
        return new Rect(ax, ay, aw, ah);
    }
    Rect heavyHitbox(){
        int aw = 34, ah = 24;
        int ax = (int)(x + (dir>0 ? w-2 : -aw));
        int ay = (int)(y + h - 36);
        return new Rect(ax, ay, aw, ah);
    }

    void takeDamage(int dmg){
        if (invulnTimer > 0) return;
        health -= dmg; if (health < 0) health = 0;
        invulnTimer = 25;
    }

    void draw(Graphics2D g2, int tick){
        if (invulnTimer>0 && (tick/3)%2==0) return;
        Art.drawKnight(g2, bounds(), dir, (attackingLight||attackingHeavy), vx, tick);
    }
}
