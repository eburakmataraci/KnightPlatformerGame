import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

class Art {

    // ===== Sprites =====
    static BufferedImage knightSprite;
    static BufferedImage enemySprite;
    static BufferedImage[] enemyByLevel = new BufferedImage[7]; // 1..6 (index 0 boş)

    static {
        loadKnight();
        loadEnemy();
    }

    static void loadKnight(String... candidates) {
        loadImageHelper(true, candidates, "/assets/knight.png", "assets/knight.png", "knight.png");
    }
    static void loadEnemy (String... candidates) {
        loadImageHelper(false, candidates, "/assets/enemy1.png", "assets/enemy1.png", "enemy1.png");
    }

    private static void loadImageHelper(boolean isKnight, String[] candidates, String... defaults) {
        String[] all = (candidates == null || candidates.length == 0) ? defaults : candidates;
        for (String path : all) {
            try {
                URL u = Art.class.getResource(path.startsWith("/") ? path : "/" + path);
                if (u != null) {
                    if (isKnight) knightSprite = ImageIO.read(u); else enemySprite = ImageIO.read(u);
                    return;
                }
                File f = new File(path.startsWith("/") ? path.substring(1) : path);
                if (f.exists()) {
                    if (isKnight) knightSprite = ImageIO.read(f); else enemySprite = ImageIO.read(f);
                    return;
                }
            } catch (IOException ignored) {}
        }
    }

    // ---- Level-bazlı düşman sprite yönetimi ----
    static void setEnemySpriteForLevel(int level, String... candidates){
        if (level < 1 || level >= enemyByLevel.length) return;
        enemyByLevel[level] = loadFirstAvailable(candidates);
        System.out.println("[Art] set L" + level + " sprite: " + (enemyByLevel[level]!=null ? "OK" : "YOK"));
    }

    static BufferedImage getEnemySpriteForLevel(int level){
        if (level >= 1 && level < enemyByLevel.length) {
            if (enemyByLevel[level] == null) {
                // Lazy auto: /assets/enemy{level}.png
                String auto = "/assets/enemy" + level + ".png";
                enemyByLevel[level] = loadFirstAvailable(auto, auto.substring(1));
                System.out.println("[Art] lazy L" + level + " -> " + (enemyByLevel[level]!=null ? "OK" : "YOK"));
            }
            if (enemyByLevel[level] != null) return enemyByLevel[level];
        }
        return enemySprite;
    }

    private static BufferedImage loadFirstAvailable(String... paths){
        if (paths == null) return null;
        for (String path : paths){
            try {
                URL u = Art.class.getResource(path.startsWith("/")? path : "/" + path);
                if (u != null) return ImageIO.read(u);
                File f = new File(path.startsWith("/")? path.substring(1) : path);
                if (f.exists()) return ImageIO.read(f);
            } catch (IOException ignored) {}
        }
        return null;
    }

    // ===== Tema API =====
    enum Theme { DESERT, SNOW, RAIN, WASTELAND, WAR, SPACE }

    static void drawBackdrop(Theme t, Graphics2D g2, double camX, double camY, int tick, int viewW, int viewH){
        switch (t){
            case DESERT:    drawDesertBackdrop(g2, camX, camY, tick, viewW, viewH); break;
            case SNOW:      drawSnowBackdrop(g2, camX, camY, tick, viewW, viewH); break;
            case RAIN:      drawRainForestBackdrop(g2, camX, camY, tick, viewW, viewH); break;
            case WASTELAND: drawWastelandBackdrop(g2, camX, camY, tick, viewW, viewH); break;
            case WAR:       drawWarBackdrop(g2, camX, camY, tick, viewW, viewH); break;
            case SPACE:     drawSpaceBackdrop(g2, camX, camY, tick, viewW, viewH); break;
        }
    }

    static void drawTile(Theme t, Graphics2D g2, Rect s, int tick){
        switch (t){
            case DESERT:    drawSandTile(g2, s, tick); break;
            case SNOW:      drawSnowTile(g2, s); break;
            case RAIN:      drawMossyStoneTile(g2, s); break;
            case WASTELAND: drawCrackedDirtTile(g2, s); break;
            case WAR:       drawTrenchWoodTile(g2, s); break;
            case SPACE:     drawMetalTile(g2, s); break;
        }
    }

    static void drawWeather(Theme t, Graphics2D g2, double camX, double camY, int tick, int viewW, int viewH){
        switch (t){
            case SNOW:  drawSnowfall(g2, camX, camY, tick, viewW, viewH); break;
            case RAIN:  drawRain(g2, camX, camY, tick, viewW, viewH); break;
            case WAR:   drawSmoke(g2, camX, camY, tick, viewW, viewH); break;
            default: break;
        }
    }

    // ===== Bayrak & Karakter/Düşman çizimi =====
    static void drawFlag(Graphics2D g2, Rect r, Color color, int tick){
        int poleX = r.x + r.w/2;
        g2.setColor(new Color(170,120,80));
        g2.fillRoundRect(poleX-3, r.y - r.h, 6, r.h + r.h/2, 6, 6);
        double wave = Math.sin(tick*0.25)*6;
        Polygon flag = new Polygon();
        flag.addPoint(poleX+3, r.y - r.h + 10);
        flag.addPoint(poleX+3 + 36, r.y - r.h + 10 + (int)wave);
        flag.addPoint(poleX+3, r.y - r.h + 28);
        g2.setColor(color); g2.fillPolygon(flag);
        g2.setColor(new Color(110,80,50));
        g2.fillRoundRect(poleX-10, r.y+ r.h-6, 20, 10, 6, 6);
    }

    static void drawKnight(Graphics2D g2, Rect b, int dir, boolean attacking, double vx, int tick) {
        // gölge
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval(b.x + b.w/2 - 12, b.y + b.h - 6, 24, 6);
        if (knightSprite == null) { g2.setColor(Color.BLUE); g2.fillRect(b.x, b.y, b.w, b.h); return; }
        double sp = Math.min(1.0, Math.abs(vx)/4.5);
        int bob = (int)Math.round(Math.sin(tick*0.25*sp)*2);
        int push = attacking ? (dir>0? 2 : -2) : 0;
        if (dir>=0) g2.drawImage(knightSprite, b.x+push, b.y+bob, b.w, b.h, null);
        else        g2.drawImage(knightSprite, b.x+b.w+push, b.y+bob, -b.w, b.h, null);
    }

    static void drawEnemyWithSprite(Graphics2D g2, Rect b, int dir, boolean attacking,
                                    int hp, int maxHp, int tick, BufferedImage sprite) {
        // gölge
        g2.setColor(new Color(0,0,0,60));
        g2.fillOval(b.x + b.w/2 - 10, b.y + b.h - 5, 20, 6);

        BufferedImage use = (sprite != null) ? sprite : enemySprite;
        if (use == null) {
            g2.setColor(Color.RED);
            g2.fillRect(b.x, b.y, b.w, b.h);
        } else {
            int bob = (int)Math.round(Math.sin(tick*0.3)*2);
            if (dir >= 0) g2.drawImage(use, b.x, b.y+bob, b.w, b.h, null);
            else          g2.drawImage(use, b.x+b.w, b.y+bob, -b.w, b.h, null);
        }

        // HP barı
        g2.setColor(new Color(0,0,0,120)); g2.fillRect(b.x, b.y-8, b.w, 5);
        int hpw = (int)(b.w * (hp/(double)maxHp));
        g2.setColor(new Color(220,60,60)); g2.fillRect(b.x, b.y-8, hpw, 5);
    }

    // --- Eski çağrılarla uyum için wrapper'lar ---
    static void drawBrute(Graphics2D g2, Rect b, int dir, boolean attacking,
                          int hp, int maxHp, int tick) {
        drawEnemyWithSprite(g2, b, dir, attacking, hp, maxHp, tick, enemySprite);
    }
    static void drawSpearman(Graphics2D g2, Rect b, int dir, boolean attacking,
                             int hp, int maxHp, int tick) {
        drawEnemyWithSprite(g2, b, dir, attacking, hp, maxHp, tick, enemySprite);
    }
    static void drawDrone(Graphics2D g2, Rect b, int dir, boolean attacking,
                          int hp, int maxHp, int tick) {
        drawEnemyWithSprite(g2, b, dir, attacking, hp, maxHp, tick, enemySprite);
    }

    // ===== Arkaplan & karo stilleri =====
    // --- DESERT ---
    static void drawDesertBackdrop(Graphics2D g2, double camX, double camY, int tick, int viewW, int viewH){
        GradientPaint sky = new GradientPaint((int)camX, (int)camY, new Color(247,183,51),
                (int)camX, (int)camY + viewH, new Color(255,236,179));
        g2.setPaint(sky); g2.fillRect((int)camX, (int)camY, viewW, viewH);
        int sunX = (int)(camX + viewW*0.75), sunY = (int)(camY + viewH*0.25);
        g2.setColor(new Color(255,244,117,220)); g2.fillOval(sunX-50, sunY-50, 100, 100);
        g2.setColor(new Color(255,244,117,80));  g2.fillOval(sunX-90, sunY-90, 180, 180);
        drawDune(g2, camX, camY, viewW, viewH, 0.25, new Color(234,198,145));
        drawDune(g2, camX, camY, viewW, viewH, 0.45, new Color(222,184,135));
        drawDune(g2, camX, camY, viewW, viewH, 0.70, new Color(210,170,120));
    }
    private static void drawDune(Graphics2D g2, double camX, double camY, int viewW, int viewH, double parallax, Color c){
        int baseY = (int)(camY + viewH*0.65 + (1-parallax)*40);
        g2.setColor(c);
        Path2D p = new Path2D.Double();
        p.moveTo(camX - 1000, baseY);
        for (int x = -1000; x <= viewW + 1000; x += 40) {
            double y = baseY + Math.sin((x + camX*parallax) * 0.005) * 18 * parallax;
            p.lineTo(camX + x, y);
        }
        p.lineTo(camX + viewW + 1000, camY + viewH + 200);
        p.lineTo(camX - 1000, camY + viewH + 200);
        p.closePath(); g2.fill(p);
    }
    static void drawSandTile(Graphics2D g2, Rect s, int tick){
        Color base = new Color(232,202,148), edge = new Color(206,176,126), hi = new Color(255,235,185);
        g2.setColor(base); g2.fillRoundRect(s.x, s.y, s.w, s.h, 10, 10);
        g2.setColor(edge); g2.drawRoundRect(s.x, s.y, s.w, s.h, 10, 10);
        g2.setColor(hi);   g2.fillRoundRect(s.x+3, s.y+3, s.w-6, 6, 8, 8);
        Random r = new Random(seed(s.x, s.y));
        g2.setColor(new Color(180,150,110,150));
        for (int i=0;i<10;i++){
            int px=s.x+6+r.nextInt(Math.max(1,s.w-12));
            int py=s.y+6+r.nextInt(Math.max(1,s.h-12));
            g2.fillOval(px,py,2,2);
        }
    }

    // --- SNOW ---
    static void drawSnowBackdrop(Graphics2D g2, double camX, double camY, int tick, int viewW, int viewH){
        GradientPaint sky = new GradientPaint((int)camX, (int)camY, new Color(190,210,235),
                (int)camX, (int)camY + viewH, new Color(230,242,255));
        g2.setPaint(sky); g2.fillRect((int)camX, (int)camY, viewW, viewH);
        g2.setColor(new Color(200,220,240));
        g2.fillRect((int)camX, (int)(camY + viewH*0.8), viewW, (int)(viewH*0.2));
    }
    static void drawSnowTile(Graphics2D g2, Rect s){
        g2.setColor(new Color(210,230,250)); g2.fillRoundRect(s.x, s.y, s.w, s.h, 10,10);
        g2.setColor(new Color(160,180,210)); g2.drawRoundRect(s.x, s.y, s.w, s.h, 10,10);
        g2.setColor(Color.WHITE); g2.fillRoundRect(s.x+2, s.y+2, s.w-4, 8, 8,8);
    }
    static void drawSnowfall(Graphics2D g2, double camX, double camY, int tick, int viewW, int viewH){
        Random r = new Random(12345);
        g2.setColor(new Color(255,255,255,200));
        for (int i=0;i<140;i++){
            int sx = (int)(camX + (i*37 + tick*2) % (viewW+200) - 100);
            int sy = (int)(camY + (r.nextInt(viewH) + tick) % (viewH));
            g2.fillOval(sx, sy, 3,3);
        }
    }

    // --- RAIN (Jungle) ---
    static void drawRainForestBackdrop(Graphics2D g2, double camX, double camY, int tick, int viewW, int viewH){
        GradientPaint sky = new GradientPaint((int)camX, (int)camY, new Color(60,90,80),
                (int)camX, (int)camY + viewH, new Color(120,160,140));
        g2.setPaint(sky); g2.fillRect((int)camX, (int)camY, viewW, viewH);
        g2.setColor(new Color(40,70,60,160));
        for (int i=0;i<6;i++){
            g2.fillRect((int)(camX + i*180 - (tick%180)), (int)(camY+viewH*0.5), 60, (int)(viewH*0.6));
        }
    }
    static void drawMossyStoneTile(Graphics2D g2, Rect s){
        g2.setColor(new Color(80,95,85)); g2.fillRoundRect(s.x, s.y, s.w, s.h, 10,10);
        g2.setColor(new Color(50,60,55)); g2.drawRoundRect(s.x, s.y, s.w, s.h, 10,10);
        g2.setColor(new Color(40,120,60,160)); g2.fillRect(s.x+4, s.y+4, s.w-8, 6);
    }
    static void drawRain(Graphics2D g2, double camX, double camY, int tick, int viewW, int viewH){
        g2.setColor(new Color(180,200,220,140));
        for (int x= (int)camX-50; x<camX+viewW+50; x+=8){
            int y = (int)(camY + ((x + tick*8) % (viewH+50)) - 50);
            g2.drawLine(x, y, x+3, y+12);
        }
    }

    // --- WASTELAND ---
    static void drawWastelandBackdrop(Graphics2D g2, double camX, double camY, int tick, int viewW, int viewH){
        GradientPaint sky = new GradientPaint((int)camX, (int)camY, new Color(130,120,110),
                (int)camX, (int)camY + viewH, new Color(170,150,130));
        g2.setPaint(sky); g2.fillRect((int)camX, (int)camY, viewW, viewH);
        g2.setColor(new Color(90,80,70));
        g2.fillRect((int)camX, (int)(camY + viewH*0.75), viewW, (int)(viewH*0.25));
    }
    static void drawCrackedDirtTile(Graphics2D g2, Rect s){
        g2.setColor(new Color(140,110,90)); g2.fillRoundRect(s.x, s.y, s.w, s.h, 10,10);
        g2.setColor(new Color(90,70,60));   g2.drawRoundRect(s.x, s.y, s.w, s.h, 10,10);
        g2.setColor(new Color(80,60,50));
        g2.drawLine(s.x+6, s.y+12, s.x+s.w-6, s.y+12);
        g2.drawLine(s.x+10, s.y+24, s.x+s.w-10, s.y+26);
    }

    // --- WAR ---
    static void drawWarBackdrop(Graphics2D g2, double camX, double camY, int tick, int viewW, int viewH){
        g2.setColor(new Color(90,90,95)); g2.fillRect((int)camX, (int)camY, viewW, viewH);
        g2.setColor(new Color(60,60,65));
        g2.fillRect((int)camX, (int)(camY + viewH*0.7), viewW, (int)(viewH*0.3));
        g2.setColor(new Color(50,50,55,180));
        for (int i=0;i<8;i++){
            int bx = (int)(camX + 60 + i*120);
            g2.fillRect(bx, (int)(camY + viewH*0.5 - (i%3)*20), 60, 80);
        }
    }
    static void drawTrenchWoodTile(Graphics2D g2, Rect s){
        g2.setColor(new Color(110,85,60)); g2.fillRoundRect(s.x, s.y, s.w, s.h, 10,10);
        g2.setColor(new Color(70,50,35));  g2.drawRoundRect(s.x, s.y, s.w, s.h, 10,10);
        for (int i=0;i<s.h; i+=8){ g2.drawLine(s.x+4, s.y+i, s.x+s.w-4, s.y+i+2); }
    }
    static void drawSmoke(Graphics2D g2, double camX, double camY, int tick, int viewW, int viewH){
        g2.setColor(new Color(50,50,55,70));
        for (int i=0;i<40;i++){
            int x = (int)(camX + (i*57 + tick*2)%(viewW+160) - 80);
            int y = (int)(camY + viewH*0.5 + Math.sin((tick+i)*0.05)*20);
            g2.fillOval(x, y, 40, 24);
        }
    }

    // --- SPACE ---
    static void drawSpaceBackdrop(Graphics2D g2, double camX, double camY, int tick, int viewW, int viewH){
        g2.setColor(new Color(10,12,25)); g2.fillRect((int)camX, (int)camY, viewW, viewH);
        g2.setColor(new Color(220,220,255));
        Random r = new Random(42);
        for (int i=0;i<180;i++){
            int sx = (int)(camX + (i*53 + tick)%(viewW+200) - 100);
            int sy = (int)(camY + r.nextInt(viewH));
            g2.fillRect(sx, sy, 1, 1);
        }
        g2.setColor(new Color(200,200,210));
        g2.fillOval((int)(camX + viewW*0.8), (int)(camY+60), 60, 60);
    }
    static void drawMetalTile(Graphics2D g2, Rect s){
        g2.setColor(new Color(90,100,120)); g2.fillRoundRect(s.x, s.y, s.w, s.h, 10,10);
        g2.setColor(new Color(50,60,80));   g2.drawRoundRect(s.x, s.y, s.w, s.h, 10,10);
        g2.setColor(new Color(150,160,180));
        g2.drawLine(s.x+6, s.y+10, s.x+s.w-6, s.y+10);
        g2.drawLine(s.x+6, s.y+22, s.x+s.w-6, s.y+22);
    }

    // ===== yardımcı =====
    private static long seed(int x, int y){
        long a = x*73856093L ^ y*19349663L;
        return a*83492791L + 0x9E3779B97F4A7C15L;
    }
}