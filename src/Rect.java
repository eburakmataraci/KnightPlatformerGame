class Rect {
    int x, y, w, h;

    Rect(int x,int y,int w,int h){
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    boolean intersects(Rect o){
        return x < o.x + o.w &&
                x + w > o.x &&
                y < o.y + o.h &&
                y + h > o.y;
    }

    boolean contains(int px, int py){
        return px >= x && px < x + w &&
                py >= y && py < y + h;
    }
}
