import java.awt.Color;

public class BlackHole extends Body {

    BlackHole(String name, double mass, Location l, double radius, Velocity v){
        super(name, mass, l, radius, v);
    }

    @Override
    Color colour(){
        return Color.BLACK;
    }

    //Black on a black background is invisible, so a black hole gets a glowing ring
    @Override
    Color halo(){
        return new Color(255, 140, 0);
    }
}