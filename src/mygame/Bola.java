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

    private NavMeshPathfinder navi;
    private NavMesh navMesh;

    private Geometry player; // Despues se reemplazara al implementar el modelo3d
    private Node bola; //TODO esto hay que instanciarlo

    private final Vector3f[] puntos = {
        new Vector3f(0, 0, 1),
        new Vector3f(0, 0, 2),
        new Vector3f(0, 0, 3)
    }; //TODO replace with actual points
    private byte lastUsed;
    private Random rnd;

    public Bola(Node playerNode, BetterCharacterControl controler, NavMeshPathfinder navi) {
        this.playerNode = playerNode;
        this.navi = navi;

        spatial = playerNode;
        playerControl = controler;

        rnd = new Random();
        lastUsed = (byte) rnd.nextInt(3);
        finalPoint = puntos[lastUsed];
        navi.clearPath();
        navi.computePath(finalPoint);
    }

    public Bola() {
        System.err.println("Empty Bola constructor has been called, we should avoid this");
    }

    @Override
    protected void controlUpdate(float tpf) {
        playerControl.setWalkDirection(Vector3f.ZERO);

        // Movimiento
        if (moving && finalPoint != null) {
            Waypoint wayPoint = navi.getNextWaypoint();

            if (wayPoint != null) {
                Vector3f direccion = wayPoint.getPosition().subtract(playerNode.getLocalTranslation());
                playerControl.setWalkDirection(direccion.normalize().mult(20));

                // Settear direccion del coche
                //Quaternion directionRot = new Quaternion();
                //directionRot.lookAt(direccion.normalize(), Vector3f.UNIT_Y);
                //player.setLocalRotation(directionRot);
                playerNode.lookAt(wayPoint.getPosition(), Vector3f.UNIT_Y);
                if (playerNode.getLocalTranslation().distance(wayPoint.getPosition()) <= 4 && !navi.isAtGoalWaypoint()) {
                    System.out.println("Next waypoint");
                    navi.goToNextWaypoint();
                }

                if (navi.isAtGoalWaypoint()) {
                    Byte temp;
                    System.out.println("AT waypoint");
                    do {
                        temp = (byte) rnd.nextInt(3);
                    } while (temp == lastUsed);
                    lastUsed = temp;
                    finalPoint = puntos[lastUsed];
                    navi.clearPath();
                    navi.computePath(finalPoint);
                }
            } else {
                System.out.println("Esta a null");
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
            temp = (byte) rnd.nextInt(3);
        } while (temp == lastUsed);
        lastUsed = temp;
        finalPoint = puntos[lastUsed];
        playerNode.setLocalTranslation(finalPoint);
        do {
            temp = (byte) rnd.nextInt(3);
        } while (temp == lastUsed);
        lastUsed = temp;
        finalPoint = puntos[lastUsed];
        navi.clearPath();
        navi.computePath(finalPoint);
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
