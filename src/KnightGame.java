import javax.swing.*;
import java.awt.*;
import java.awt.Point;


public class KnightGame extends JFrame {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Knight Platformer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ICON EKLEME
        ImageIcon icon = new ImageIcon("src/assets/icon.png"); // kendi dosya yolun
        frame.setIconImage(icon.getImage());

        frame.add(new GamePanel());
        frame.pack();
        // 🔽 Pencereyi tam ekran yap (başlık çubuğu kalır, sadece maksimize)
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
