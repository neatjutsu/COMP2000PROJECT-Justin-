import java.awt.Color;

public class Body {
       String  name;
       double mass;
       Location l;
       double radius ;
       Velocity v;

       Body(String name , double mass, Location l, double radius ,Velocity v){
        if (name == null || name.trim().isEmpty()){
            throw new InvalidBodyException("A body needs a name");
        }
        if (mass <= 0){
            throw new InvalidBodyException(name + " needs a positive mass, got " + mass);
        }
        if (radius <= 0){
            throw new InvalidBodyException(name + " needs a positive radius, got " + radius);
        }
        this.name=name;
        this.mass=mass;
        this.l=l;
        this.radius=radius;
        this.v=v;

    }

    Color colour(){
        return Color.LIGHT_GRAY;
    }

    //Whether the simulation writes this body's name next to it
    boolean showLabel(){
        return true;
    }

    //An optional glowing ring drawn around the body. Most bodies have none.
    Color halo(){
        return null;
    }
}
