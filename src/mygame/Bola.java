/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.ai.navmesh.NavMesh;
import com.jme3.ai.navmesh.NavMeshPathfinder;
import com.jme3.ai.navmesh.Path.Waypoint;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.collision.CollisionResults;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import java.io.IOException;
import java.util.Random;

/**
 *
 * @author darylfed
 */
public class Bola extends AbstractControl {

    private BetterCharacterControl playerControl;

    private Node playerNode; // Nodo donde estara el jugador en el mapa

    private Vector3f finalPoint;
    private boolean moving = false;
    private byte debug = 1; //0= no output; 1= debug output; 2=perFrameOutput, verbosity intensifies

    private NavMeshPathfinder navi;
    private NavMesh navMesh;

    private Geometry player; // Despues se reemplazara al implementar el modelo3d
    private Node bola; //TODO esto hay que instanciarlo

    private final Vector3f[] puntos = {
        new Vector3f(79.885666f, 0, -56.16912f),
        new Vector3f(53.739586f, 0, 102.26378f),
        new Vector3f(-88.340645f, 0, 94.01254f),
        new Vector3f(-62.67897f, 0, 10.206712f)
    };
    private byte lastUsed;
    private Random rnd;

    public Bola(Node playerNode, BetterCharacterControl controler, NavMeshPathfinder navi) {
        this.playerNode = playerNode;
        this.navi = navi;

        spatial = playerNode;
        playerControl = controler;

        rnd = new Random();
        moveToNextPoint();
    }

    public Bola() {
        System.err.println("Empty Bola constructor has been called, we should avoid this");
    }

    private void moveToNextPoint() {
        byte temp;
        do {
            temp = (byte) rnd.nextInt(puntos.length);
        } while (temp == lastUsed);
        lastUsed = temp;
        finalPoint = puntos[lastUsed];
        navi.clearPath();
        navi.computePath(finalPoint);
        debugMsg("Me dirijo al punto " + lastUsed,1);
        moving = true;
    }

    @Override
    protected void controlUpdate(float tpf) {
        playerControl.setWalkDirection(Vector3f.ZERO);
        debugMsg("Hola, deberia moverme 0",2);
        // Movimiento
        if (moving && finalPoint != null) {
            Waypoint wayPoint = navi.getNextWaypoint();
            debugMsg("Hola, deberia moverme 1",2);

            if (wayPoint != null) {
                debugMsg("Hola, deberia moverme 2",2);

                Vector3f direccion = wayPoint.getPosition().subtract(playerNode.getLocalTranslation());
                playerControl.setWalkDirection(direccion.normalize().mult(20));

                // Settear direccion del coche
                //Quaternion directionRot = new Quaternion();
                //directionRot.lookAt(direccion.normalize(), Vector3f.UNIT_Y);
                //player.setLocalRotation(directionRot);
                playerNode.lookAt(wayPoint.getPosition(), Vector3f.UNIT_Y);
                if (playerNode.getLocalTranslation().distance(wayPoint.getPosition()) <= 4 && !navi.isAtGoalWaypoint()) {
                    debugMsg("Next waypoint",1);
                    navi.goToNextWaypoint();
                }

                if (navi.isAtGoalWaypoint()) {
                    Byte temp;
                    debugMsg("AT waypoint",1);
                    moveToNextPoint();
                }
            } else {
                debugMsg("Esta a null",1);
            }

        }
    }

    public void computeNewPath(Vector3f finalPoint) {
        this.finalPoint = finalPoint;
        navi.setPosition(playerNode.getLocalTranslation());
        navi.computePath(finalPoint);
    }

    public void onCollisionWithPlayer() {
        Byte temp;
        do {
            temp = (byte) rnd.nextInt(puntos.length);
        } while (temp == lastUsed);
        lastUsed = temp;
        finalPoint = puntos[lastUsed];
        playerNode.setLocalTranslation(finalPoint);
        debugMsg("Teletransportado al punto " + lastUsed,1);
        moveToNextPoint();

    }

    public BetterCharacterControl getPlayerControl() {
        return playerControl;
    }

    public void setPlayerControl(BetterCharacterControl playerControl) {
        this.playerControl = playerControl;
    }

    public Vector3f getFinalPoint() {
        return finalPoint;
    }

    public void setFinalPoint(Vector3f finalPoint) {
        this.finalPoint = finalPoint;
    }

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }
    
    private void debugMsg(String s, int verbo){
        if(debug>=verbo) System.out.println(s);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        //Only needed for rendering-related operations,
        //not called when spatial is culled.
    }

    public Control cloneForSpatial(Spatial spatial) {
        Bola control = new Bola();
        //TODO: copy parameters to new Control
        return control;
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
    }

}
