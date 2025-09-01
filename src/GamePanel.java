import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {

    // --- Görüntü (oyunun orijinal hedef çözünürlüğü) ---
    public static final int VIEW_W = 1024;
    public static final int VIEW_H = 576;
    private static final int BASE_W = 1024; // scale baz alınır
    private static final int BASE_H = 576;

    public static final int TILE = 48;

    // Timer - açık nitelikli (javax.swing.Timer)
    private final javax.swing.Timer timer = new javax.swing.Timer(1000 / 60, this);

    // --- Input ---
    private boolean left, right, jumpPressed, jumpHeld, atkLightPressed, atkHeavyPressed, rollPressed, pause;

    // --- Kamera ---
    private double camX = 0, camY = 0;

    // --- Dünya ---
    private Player player;
    private List<Rect> solids = new ArrayList<>();
    private List<Enemy> enemies = new ArrayList<>();
    private List<Rect> checkpoints = new ArrayList<>();
    private Rect goal;

    // --- Level yönetimi ---
    private int currentLevel = 0;                 // 0-based
    private final List<Level> levels = new ArrayList<>();
    private boolean[] unlocked;                   // 6 level
    private Art.Theme[] levelThemes;              // her level teması
    private Art.Theme currentTheme = Art.Theme.DESERT;

    // --- Zaman ---
    private int tick = 0;

    // --- Durum ---
    private enum State { LEVEL_SELECT, RUNNING }
    private State state = State.LEVEL_SELECT;

    // --- Menü UI ---
    private final List<Rect> levelButtons = new ArrayList<>();
    private int hoverIndex = -1;

    // --- Menü partikül arka planı ---
    private static class Particle { float x,y,vx,vy,size,life,hue; }
    private final ArrayList<Particle> particles = new ArrayList<>();
    private final Random prnd = new Random();

    // --- Oyun içi efekt parçacıkları ---
    private final ArrayList<Particle> fxWeather  = new ArrayList<>();
    private final ArrayList<Particle> fxSplashes = new ArrayList<>();
    private final ArrayList<Particle> fxHits     = new ArrayList<>();

    // --- COVER ölçekleme için mouse dönüşüm değerleri ---
    private double screenScale = 1.0;
    private int screenOffX = 0, screenOffY = 0;

    GamePanel() {
        setPreferredSize(new Dimension(VIEW_W, VIEW_H));
        setBackground(Color.black);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        buildLevels();
        initThemes();
        initUnlocks();
        buildLevelButtons();
        registerEnemySprites();
        initMenuParticles();

        loadLevel(0);
        state = State.LEVEL_SELECT; // menüde başla
        timer.start();
    }

    // ---------------------- Temalar / Unlock / UI ----------------------

    private void initThemes() {
        levelThemes = new Art.Theme[]{
                Art.Theme.DESERT, Art.Theme.SNOW, Art.Theme.RAIN,
                Art.Theme.WASTELAND, Art.Theme.WAR, Art.Theme.SPACE
        };
    }

    private void initUnlocks() {
        unlocked = new boolean[6];
        unlocked[0] = true; // Level 1 açık
    }

    private void buildLevelButtons() {
        levelButtons.clear();
        int cols = 3, rows = 2;
        int bw = 180, bh = 110, pad = 32;
        int startX = (VIEW_W - (cols*bw + (cols-1)*pad)) / 2;
        int startY = (VIEW_H - (rows*bh + (rows-1)*pad)) / 2 + 20;
        for (int r=0; r<rows; r++)
            for (int c=0; c<cols; c++)
                levelButtons.add(new Rect(startX + c*(bw+pad), startY + r*(bh+pad), bw, bh));
    }

    private Color themeColor(int idx){
        switch (levelThemes[idx]){
            case DESERT: return new Color(247,183,51);
            case SNOW: return new Color(180,210,255);
            case RAIN: return new Color(110,180,140);
            case WASTELAND: return new Color(170,150,130);
            case WAR: return new Color(240,120,80);
            case SPACE: return new Color(120,140,220);
            default: return Color.WHITE;
        }
    }

    // --- Menü partikülleri ---
    private void initMenuParticles(){
        particles.clear();
        for (int i=0;i<120;i++) particles.add(newParticle());
    }
    private Particle newParticle(){
        Particle p = new Particle();
        p.x = prnd.nextInt(VIEW_W); p.y = prnd.nextInt(VIEW_H);
        p.vx = (prnd.nextFloat()-0.5f)*0.3f; p.vy = 0.2f + prnd.nextFloat()*0.5f;
        p.size = 1.5f + prnd.nextFloat()*3f;
        p.life = 120 + prnd.nextFloat()*240f;
        p.hue = prnd.nextFloat();
        return p;
    }
    private void updateParticles(){
        for (int i=0;i<particles.size();i++){
            Particle p = particles.get(i);
            p.x += p.vx; p.y += p.vy; p.life -= 1f;
            if (p.y > VIEW_H+10 || p.life<=0){
                particles.set(i, newParticle());
                particles.get(i).y = -10;
                particles.get(i).x = prnd.nextInt(VIEW_W);
            }
        }
    }

    // ---------------------- Level Tasarımı ----------------------

    private void buildLevels() {
        String[] L1 = new String[]{
                ".................................................................................",
                ".................................................................................",
                "......................................................E..........................",
                ".............................................WW..................................",
                "..........................................WW..W..................................",
                ".......................................WW....W..................C................",
                ".................................WW...W......W...............WWWW................",
                "....S.........................WW..W...W......W.............W.....W......G........",
                "...........................WW.....W..W.......W...........W.......W...............",
                ".........................WW........W.W.......W.........WW.........WW.............",
                "......................WW.........................................................",
                "WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW"
        };
        String[] L2 = new String[]{
                "................................................................................",
                "..................................................E.............................",
                "..............................................WW................................",
                "..........................................WW..W.W...............................",
                "......................................WW..W....W..WW.....C......................",
                ".................................WW..W....W.......W..WW.........................",
                "............................WW..W......E..W...........W..WW.............G.......",
                "...S...................WW..W............W..............W....W...................",
                "...................WW..W................W...............W....W..................",
                "...............WW..W....................W................W....W.................",
                "..........WW....................................................................",
                "WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW"
        };
        String[] L3 = new String[]{
                "................................................................................",
                "...........................................E....................................",
                "......................................WWWWWWWW..................................",
                ".................................WW..W........W.................C...............",
                "............................WW..W....W..E.....W.................................",
                ".......................WW..W......WWWWWW......W.........................WW......",
                "..................WW..W.......................W.....................WW..W..WW...",
                "...S..........WW..W...........................W..............C....W....W....G...",
                "...........WW..W..............................W................W..W....W........",
                ".......WW..W..................................W..........W...W.W..W....W........",
                "....WW................................................W....W....................",
                "WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW"
        };
        String[] L4 = new String[]{
                "................................................................................",
                "..............................................E.................................",
                "...............................WWW......................WWW.....................",
                "...........................WWW.....WWW...........W.WWW......WWW...........C.....",
                ".......................WWW...................WW.............WWW.................",
                "...................WWW.................W....W.W....................WWW..........",
                "...............WWW........................W...........................WWW..G....",
                "...S.......WWW...........................W................................WWW...",
                "........WWW.............................W...................................WWW.",
                "....WWW...............................W.........................................",
                ".....................................W..........................................",
                "WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW"
        };
        String[] L5 = new String[]{
                "................................................................................",
                "....................................E.................E.........................",
                "...............................WWWWWW.............WWWWWW..................C.....",
                "..........................WW..W......W.......WW..W......W.......................",
                ".....................WW..W....W..E...W..WW..W....W..E...W.......................",
                "................WW..W......WWWWWWWWWW..W..W..WWWWWWWWWW..W..............WW......",
                "...........WW..W........................W..W..............W........WW..W..WW....",
                "...S...WW..W..............................................W...C..W....W....G....",
                "......W....W..............................................W.....W....W..........",
                "......W....W..............................................W...WW......WW........",
                "................................................................................",
                "WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW"
        };
        String[] L6 = new String[]{
                "................................................................................",
                "..............................E.................................................",
                "...................WW..WW..WW..WW..WW..WW..WW..WW..C............................",
                "...............WW..W..............................W..WW.........................",
                "..........WW..W....W............E.................W....W.................G......",
                ".....WW..W......WWWWWW.........................WWWWWW....W......................",
                "...S..W............................................................WW...........",
                ".....W..........................................................WW..W...........",
                "................................................................W....W..........",
                ".................................................................W..WW..........",
                "................................................................................",
                "WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW"
        };

        levels.clear();
        levels.add(new Level(L1, TILE));
        levels.add(new Level(L2, TILE));
        levels.add(new Level(L3, TILE));
        levels.add(new Level(L4, TILE));
        levels.add(new Level(L5, TILE));
        levels.add(new Level(L6, TILE));
    }

    private void registerEnemySprites(){
        Art.setEnemySpriteForLevel(1, "/assets/enemy1.png", "assets/enemy1.png", "enemy1.png");
        Art.setEnemySpriteForLevel(2, "/assets/enemy2.png", "assets/enemy2.png", "enemy2.png");
        Art.setEnemySpriteForLevel(3, "/assets/enemy3.png", "assets/enemy3.png", "enemy3.png");
        Art.setEnemySpriteForLevel(4, "/assets/enemy4.png", "assets/enemy4.png", "enemy4.png");
        Art.setEnemySpriteForLevel(5, "/assets/enemy5.png", "assets/enemy5.png", "enemy5.png");
        Art.setEnemySpriteForLevel(6, "/assets/enemy6.png", "assets/enemy6.png", "enemy6.png"); // varsa

        for (int i=2; i<=6; i++){
            System.out.println("CHECK L"+i+": " + (Art.getEnemySpriteForLevel(i) != null ? "OK" : "YOK"));
        }
    }

    // ---------------------- Level Yükleme ----------------------

    private void loadLevel(int idx) {
        currentLevel = idx;
        Level lv = levels.get(idx);

        this.solids = new ArrayList<>(lv.solids);
        this.enemies = new ArrayList<>();
        this.checkpoints = new ArrayList<>();
        this.goal = null;

        currentTheme = (levelThemes != null && idx < levelThemes.length) ? levelThemes[idx] : Art.Theme.DESERT;

        int startX = lv.start.x, startY = surfaceY(lv.start.x + TILE/2);
        this.player = new Player(startX, Math.max(0, startY - 48));
        this.player.lives = 3;
        this.player.checkpoint = new Point((int)Math.round(player.x), (int)Math.round(player.y));

        for (Point epos : lv.enemySpawns) {
            int sx = epos.x, syTop = surfaceY(sx + 24);
            int ey = Math.max(0, syTop - 46);
            int levelNumber = idx + 1;

            if (levelNumber >= 2 && levelNumber <= 6) {
                java.awt.image.BufferedImage spr = Art.getEnemySpriteForLevel(levelNumber);
                enemies.add(new Swordsman(sx, ey, 38, 46, spr));
            } else {
                // Level 1: temaya göre çeşit
                switch (currentTheme) {
                    case WASTELAND: enemies.add(new Brute(sx, ey, 44, 52)); break;
                    case SNOW:
                    case WAR:       enemies.add(new Spearman(sx, ey, 36, 46)); break;
                    case SPACE:     enemies.add(new Drone(sx, ey - 40, 36, 28)); break;
                    default:        enemies.add(new Swordsman(sx, ey, 38, 46)); break;
                }
            }
        }

        for (Rect c : lv.checkpoints) {
            int cx = c.x + c.w/2;
            int top = surfaceY(cx);
            Rect snapC = new Rect(cx - 10, Math.max(0, top - c.h), 20, c.h);
            checkpoints.add(snapC);
        }
        if (lv.goal != null) {
            int gx = lv.goal.x + lv.goal.w/2;
            int top = surfaceY(gx);
            goal = new Rect(gx - 10, Math.max(0, top - lv.goal.h), 20, lv.goal.h);
        }

        this.state = State.LEVEL_SELECT;
        this.camX = 0; this.camY = 0; this.tick = 0;
    }

    private int surfaceY(int worldX) {
        int best = Integer.MAX_VALUE;
        for (Rect s : solids) {
            if (worldX >= s.x && worldX < s.x + s.w) if (s.y < best) best = s.y;
        }
        if (best == Integer.MAX_VALUE) best = VIEW_H + 200;
        return best;
    }

    // ---------------------- Oyun Döngüsü ----------------------

    @Override public void actionPerformed(ActionEvent e) {
        tick++;
        if (state == State.RUNNING) updateRunning();
        else updateParticles();
        repaint();
    }

    private void updateRunning() {
        if (pause) return;

        // --- Yatay hareket & roll ---
        double ax = 0;
        if (left)  ax -= player.speed;
        if (right) ax += player.speed;
        if (player.rolling) ax = player.dir * player.rollSpeed;
        player.vx = lerp(player.vx, ax, player.rolling ? 0.35 : 0.2);

        // --- Yerçekimi / zıplama (coyote + buffer) ---
        player.vy += player.gravity;
        if (player.onGround) player.coyote = 8; else if (player.coyote > 0) player.coyote--;
        if (jumpPressed) { player.jumpBuffer = 8; jumpPressed = false; }
        if (player.jumpBuffer > 0) player.jumpBuffer--;
        if (!player.rolling && player.jumpBuffer > 0 && player.coyote > 0) {
            player.vy = -player.jumpStrength; player.onGround = false; player.coyote = 0; player.jumpBuffer = 0;
        }
        if (!jumpHeld && player.vy < 0) player.vy += player.gravity * 0.6;

        // --- Saldırılar / roll ---
        if (atkLightPressed && player.lightCD == 0 && !player.rolling) {
            player.lightTimer = player.LIGHT_WINDOW; player.lightCD = player.LIGHT_COOLDOWN; player.attackingLight = true;
        }
        if (atkHeavyPressed && player.heavyCD == 0 && !player.rolling) {
            player.heavyTimer = player.HEAVY_WINDOW; player.heavyCD = player.HEAVY_COOLDOWN; player.attackingHeavy = true;
        }
        if (rollPressed && player.rollCD == 0 && !player.rolling && player.onGround) {
            player.rolling = true; player.rollTimer = player.ROLL_WINDOW; player.rollCD = player.ROLL_COOLDOWN;
            player.invulnTimer = player.ROLL_IFRAMES; player.vx = player.dir * player.rollSpeed;
        }
        rollPressed = false;

        // --- Zamanlayıcılar ---
        if (player.lightCD>0) player.lightCD--;
        if (player.heavyCD>0) player.heavyCD--;
        if (player.rollCD>0)  player.rollCD--;
        if (player.lightTimer>0){ player.lightTimer--; if (player.lightTimer==0) player.attackingLight=false; }
        if (player.heavyTimer>0){ player.heavyTimer--; if (player.heavyTimer==0) player.attackingHeavy=false; }
        if (player.rollTimer>0) { player.rollTimer--; if (player.rollTimer==0) player.rolling=false; }
        if (player.invulnTimer>0) player.invulnTimer--;

        // --- Çarpışma X ---
        player.x += player.vx;
        Rect pb = player.bounds();
        for (Rect s : solids) {
            if (pb.intersects(s)) {
                if (player.vx > 0) player.x = s.x - player.w; else if (player.vx < 0) player.x = s.x + s.w;
                player.vx = 0; pb = player.bounds();
            }
        }

        // --- Çarpışma Y ---
        player.y += player.vy; pb = player.bounds(); player.onGround = false;
        for (Rect s : solids) {
            if (pb.intersects(s)) {
                if (player.vy > 0) { player.y = s.y - player.h; player.vy = 0; player.onGround = true; }
                else if (player.vy < 0) { player.y = s.y + s.h; player.vy = 0; }
                pb = player.bounds();
            }
        }

        // --- Checkpoint / Goal ---
        for (Rect c : checkpoints) if (pb.intersects(c)) player.checkpoint = new Point(c.x + c.w/2 - player.w/2, c.y - player.h);
        if (goal != null && pb.intersects(goal)) {
            if (currentLevel + 1 < levels.size()) unlocked[currentLevel + 1] = true;
            state = State.LEVEL_SELECT;
        }

        // --- Düşmanlar ---
        for (Enemy en : enemies) {
            en.update(solids, player, tick);

            if (en.isAttacking(tick) && en.attackHitbox().intersects(pb) && player.invulnTimer == 0) {
                player.takeDamage(en.damageOnHit);
                int kdir = (player.x + player.w/2) < (en.x + en.w/2) ? -1 : 1;
                player.vx = 5.0 * kdir; player.vy = -6.0;
            }

            if ((player.attackingLight && player.lightHitbox().intersects(en.bounds())) ||
                    (player.attackingHeavy && player.heavyHitbox().intersects(en.bounds()))) {
                int dmg = player.attackingHeavy ? player.HEAVY_DAMAGE : player.LIGHT_DAMAGE;
                en.takeDamage(dmg);
                en.x += (player.dir>0 ? 1 : -1) * 6;
                spawnHitParticleAt(en.bounds());
            }
        }
        enemies.removeIf(e -> !e.alive);

        // --- Ölüm / Respawn ---
        if (player.health <= 0) {
            player.lives--;
            if (player.lives > 0) respawnPlayer(); else state = State.LEVEL_SELECT;
        }

        // --- Kamera ---
        double targetCamX = player.x + player.w/2.0 - VIEW_W/2.0;
        double targetCamY = player.y + player.h/2.0 - VIEW_H/2.0;
        camX = lerp(camX, targetCamX, 0.12);
        camY = lerp(camY, targetCamY, 0.12);

        if (player.y > VIEW_H + 800) player.health = 0;
    }

    private void respawnPlayer() {
        player.health = player.maxHealth;
        player.vx = player.vy = 0;
        player.x = player.checkpoint.x;
        player.y = player.checkpoint.y;
        player.invulnTimer = 60;
        player.rolling = false;
        player.attackingLight = player.attackingHeavy = false;
        player.lightTimer = player.heavyTimer = 0;
    }

    private static double lerp(double a, double b, double t){ return a + (b-a)*t; }

    // ---------------------- Çizim ----------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- COVER ölçekleme (ekranı doldur, taşanı kırp) ---
        double scaleX = getWidth()  / (double) BASE_W;
        double scaleY = getHeight() / (double) BASE_H;
        double scale  = Math.max(scaleX, scaleY); // COVER: şerit yok, kırpma olabilir

        int drawW = (int) Math.round(BASE_W * scale);
        int drawH = (int) Math.round(BASE_H * scale);

        int offX = (getWidth()  - drawW) / 2;
        int offY = (getHeight() - drawH) / 2;

        // Mouse dönüşümünde kullanmak üzere sakla
        this.screenScale = scale;
        this.screenOffX  = offX;
        this.screenOffY  = offY;

        // Görünen alan dışına taşanı kırp
        g2.setClip(0, 0, getWidth(), getHeight());

        // Ölçek ve ofset uygula
        g2.translate(offX, offY);
        g2.scale(scale, scale);

        if (state == State.LEVEL_SELECT) {
            drawLevelSelect(g2);
            g2.dispose();
            return;
        }

        // Dünya çizimi
        g2.translate(-camX, -camY);

        Art.drawBackdrop(currentTheme, g2, camX, camY, tick, BASE_W, BASE_H);
        for (Rect s : solids) Art.drawTile(currentTheme, g2, s, tick);

        for (Rect c : checkpoints) Art.drawFlag(g2, c, new Color(60,140,255), tick);
        if (goal != null) Art.drawFlag(g2, goal, new Color(220,60,60), tick);

        for (Enemy en : enemies) en.draw(g2, tick);
        player.draw(g2, tick);

        // Efektler (istemezsen bu üç satırı kapatabilirsin)
        drawParticlesWorld(g2, fxWeather);
        drawParticlesWorld(g2, fxSplashes);
        drawParticlesWorld(g2, fxHits);

        // HUD
        drawUI(g2);

        g2.dispose();
    }

    // --- Şık seviye seçimi ---
    private void drawLevelSelect(Graphics2D g2) {
        GradientPaint gp = new GradientPaint(0, 0, new Color(30,17,60), 0, VIEW_H, new Color(9,25,70));
        g2.setPaint(gp); g2.fillRect(0,0,VIEW_W,VIEW_H);

        for (Particle p : particles) {
            Color c = Color.getHSBColor(p.hue, 0.5f, 1.0f);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 120));
            g2.fillOval((int)p.x, (int)p.y, (int)p.size, (int)p.size);
        }

        String title = "SEVİYE SEÇ";
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 46f));
        int tw = g2.getFontMetrics().stringWidth(title);
        int tx = VIEW_W/2 - tw/2, ty = 88;
        g2.setColor(new Color(0,0,0,120)); g2.drawString(title, tx+3, ty+3);
        g2.setColor(new Color(255,220,140)); g2.drawString(title, tx, ty);

        for (int i=0; i<levelButtons.size(); i++) {
            Rect b = levelButtons.get(i);
            boolean isUnlocked = i < unlocked.length && unlocked[i];
            boolean hover = (i == hoverIndex) && isUnlocked;

            if (hover){
                g2.setColor(new Color(255,240,180,80));
                g2.fillRoundRect(b.x-6, b.y-6, b.w+12, b.h+12, 24,24);
            }

            g2.setColor(new Color(0,0,0,140));
            g2.fillRoundRect(b.x+6, b.y+8, b.w, b.h, 22,22);

            Color glass = isUnlocked ? new Color(255,255,255,55) : new Color(160,160,160,45);
            g2.setColor(glass);
            g2.fillRoundRect(b.x, b.y, b.w, b.h, 22,22);
            g2.setColor(new Color(255,255,255,100));
            g2.drawRoundRect(b.x, b.y, b.w, b.h, 22,22);

            Color strip = themeColor(i);
            g2.setColor(new Color(strip.getRed(), strip.getGreen(), strip.getBlue(), 160));
            g2.fillRoundRect(b.x, b.y, b.w, 18, 22,22);

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 34f));
            String label = String.valueOf(i+1);
            int nw = g2.getFontMetrics().stringWidth(label);
            g2.setColor(new Color(0,0,0,120)); g2.drawString(label, b.x + b.w/2 - nw/2 + 2, b.y + b.h/2 + 14 + 2);
            g2.setColor(isUnlocked ? Color.WHITE : new Color(220,220,220));
            g2.drawString(label, b.x + b.w/2 - nw/2, b.y + b.h/2 + 14);

            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 13f));
            String tname = levelThemes[i].name();
            g2.setColor(new Color(255,255,255,220));
            g2.drawString(tname, b.x + 12, b.y + 14);

            if (!isUnlocked){
                g2.setColor(new Color(10,10,15,140));
                g2.fillRoundRect(b.x, b.y, b.w, b.h, 22,22);
                g2.setColor(new Color(255,215,130));
                int cx = b.x + b.w - 36, cy = b.y + 26;
                g2.drawOval(cx, cy, 18, 18);
                g2.fillRect(cx+5, cy+12, 10, 14);
            }
        }

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
        g2.setColor(new Color(230,230,240));
        g2.drawString("Tıkla: seviye gir  •  Kilit: önceki bölümü bitir  •  Enter: son açık seviyeyi başlat", 24, VIEW_H - 22);
    }

    private void drawUI(Graphics2D g2) {
        int barW = 220; int barH = 16; int x = (int)camX + 20; int y = (int)camY + 20;
        g2.setColor(new Color(30,30,30,160)); g2.fillRoundRect(x-6, y-6, barW+12, barH+12, 10, 10);
        g2.setColor(new Color(80,80,80)); g2.fillRoundRect(x, y, barW, barH, 8, 8);
        int hpw = (int)(barW * (player.health/(double)player.maxHealth));
        g2.setColor(new Color(220,70,70)); g2.fillRoundRect(x, y, hpw, barH, 8, 8);
        g2.setColor(Color.WHITE); g2.drawString("Can: " + player.health + "/" + player.maxHealth, x+8, y+13);
        g2.drawString("Hak: "+player.lives, x, y+34);
        g2.drawString("←/→: yürü  Space: zıpla  J: hafif  K: ağır  L: yuvarlan  P: durdur  Enter: menü", x, y+52);
    }

    private void drawParticlesWorld(Graphics2D g2, ArrayList<Particle> list){
        for (Particle p : list){
            int alpha = 220; // basit görünürlük
            g2.setColor(new Color(255,255,255, alpha));
            int sz = Math.max(1, (int)p.size);
            g2.fillOval((int)(p.x - sz/2), (int)(p.y - sz/2), sz, sz);
        }
    }

    private void spawnHitParticleAt(Rect at){
        Particle p = new Particle();
        p.x = at.x + at.w/2f;
        p.y = at.y + at.h/2f;
        p.size = 3f; p.hue = 0f;
        fxHits.add(p);
    }

    // ---------------------- Input ----------------------

    @Override public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (state == State.LEVEL_SELECT) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                for (int i=unlocked.length-1; i>=0; i--) if (unlocked[i]) { startLevel(i); break; }
            }
            return;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A: case KeyEvent.VK_LEFT:  left = true;  break;
            case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: right = true; break;
            case KeyEvent.VK_SPACE: jumpPressed = true; jumpHeld = true; break;
            case KeyEvent.VK_J:     atkLightPressed = true; break;
            case KeyEvent.VK_K:     atkHeavyPressed = true; break;
            case KeyEvent.VK_L:     rollPressed = true; break;
            case KeyEvent.VK_P:     pause = !pause; break;
            case KeyEvent.VK_ENTER: state = State.LEVEL_SELECT; break;
        }
        if (left && !right) player.dir = -1; else if (right && !left) player.dir = 1;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (state == State.LEVEL_SELECT) return;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A: case KeyEvent.VK_LEFT:  left = false;  break;
            case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: right = false; break;
            case KeyEvent.VK_SPACE: jumpHeld = false; break;
            case KeyEvent.VK_J:     atkLightPressed = false; break;
            case KeyEvent.VK_K:     atkHeavyPressed = false; break;
        }
        if (left && !right) player.dir = -1; else if (right && !left) player.dir = 1;
    }

    // --- Mouse ---
    @Override
    public void mouseClicked(MouseEvent e) {
        if (state != State.LEVEL_SELECT) return;

        // Ekran -> sahne (1024x576) dönüşümü
        int mx = (int) Math.round((e.getX() - screenOffX) / screenScale);
        int my = (int) Math.round((e.getY() - screenOffY) / screenScale);

        for (int i=0; i<levelButtons.size(); i++) {
            Rect b = levelButtons.get(i);
            if (b.contains(mx, my) && i < unlocked.length && unlocked[i]) { startLevel(i); break; }
        }
    }
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
        if (state != State.LEVEL_SELECT) return;
        hoverIndex = -1;

        // Ekran -> sahne dönüşümü
        int mx = (int) Math.round((e.getX() - screenOffX) / screenScale);
        int my = (int) Math.round((e.getY() - screenOffY) / screenScale);

        for (int i=0;i<levelButtons.size();i++){
            Rect b = levelButtons.get(i);
            if (b.contains(mx, my)) { hoverIndex = i; break; }
        }
    }
    @Override public void mouseDragged(MouseEvent e) {
        if (state != State.LEVEL_SELECT) return;
        mouseMoved(e); // sürüklerken de hover güncellensin
    }

    private void startLevel(int idx){
        loadLevel(idx);
        state = State.RUNNING;
        camX = player.x - VIEW_W/2.0;
        camY = player.y - VIEW_H/2.0;
        requestFocusInWindow();
    }
}
