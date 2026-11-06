public class Physics {
    static final double G=3.96e-17;// Please dont change as this is my final calculation for gravitational constant acording to scale ~ Leon Prince :)

    static Velocity calculateGravity(Body body,Body source){
        double dx=source.l.x-body.l.x;
        double dy=source.l.y-body.l.y;
        double distance= Math.sqrt(dx*dx+dy*dy);
        double acceleration=G * source.mass / (distance * distance);
        double ax =acceleration * dx/distance;
        double ay=acceleration * dy/distance;
        return new Velocity(ax,ay);

    }
    //Velocity a body needs to move in a circle around `centre`: speed = sqrt(G*M/r),
    //directed at right angles to the line between them. Reproduces the team's
    //hand-calculated Earth value (6.29e-7) so every planet can orbit, not just Earth.
    static Velocity circularOrbit(Body body, Body centre){
        double dx = body.l.x - centre.l.x;
        double dy = body.l.y - centre.l.y;
        double r = Math.sqrt(dx*dx + dy*dy);
        double speed = Math.sqrt(G * centre.mass / r);
        return new Velocity(centre.v.vx - dy / r * speed, centre.v.vy + dx / r * speed);
    }

    static void updateBody(Body body ,Velocity acceleration, double dt){
        body.v.vx+=acceleration.vx * dt;
        body.v.vy+=acceleration.vy * dt ;

        body.l.x+=body.v.vx * dt;
        body.l.y+=body.v.vy * dt;
    }

    
    
    
}
