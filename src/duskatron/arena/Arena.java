package duskatron.arena;

public class Arena {

    private final double width;
    private final double height;
    // private double margin;

    public Arena(double w, double h){
        this.width = w;
        this.height = h;
        // this.margin = margin;
    }

    //public void setMargin(double margin) { this.margin = margin; }

    //public double getMargin() { return margin; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
}
