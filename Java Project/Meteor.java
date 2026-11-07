public class Meteor extends Body {

    Meteor(String name, double mass, Location l, double radius, Velocity v) throws InvalidBodyException {
        super(name, mass, l, radius, v);
    }

    //There can be hundreds of meteors, and their labels would overlap into a mess
    @Override
    boolean showLabel(){
        return false;
    }
}
