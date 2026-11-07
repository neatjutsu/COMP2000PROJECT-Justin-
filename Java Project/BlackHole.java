import java.awt.Color;

public class BlackHole extends Body {

    BlackHole(String name, double mass, Location l, double radius, Velocity v){
        super(name, mass, l, radius, v);
    }

    @Override
    Color colour(){
        return Color.BLACK;
    }
}