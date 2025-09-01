import java.util.ArrayList;
import java.util.List;

class Level {
    final List<Rect> solids = new ArrayList<>();
    final List<Point> enemySpawns = new ArrayList<>();
    final List<Rect> checkpoints = new ArrayList<>();
    Rect goal = null;
    Point start = new Point(0,0);

    Level(String[] map, int tile) {
        if (map == null || map.length == 0) return;

        for (int y = 0; y < map.length; y++) {
            String row = map[y];
            if (row == null) row = "";
            final int colsThisRow = row.length();

            for (int x = 0; x < colsThisRow; x++) {
                char c = row.charAt(x);
                int wx = x * tile, wy = y * tile;

                switch (c) {
                    case 'W': solids.add(new Rect(wx, wy, tile, tile)); break;
                    case 'S': start = new Point(wx, wy); break;
                    case 'E': enemySpawns.add(new Point(wx, wy)); break;
                    case 'C': checkpoints.add(new Rect(wx, wy, tile, tile)); break;
                    case 'G': goal = new Rect(wx, wy, tile, tile); break;
                    default: break;
                }
            }
        }
    }
}
