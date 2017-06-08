/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.ai.navmesh.NavMesh;
import com.jme3.ai.navmesh.NavMeshPathfinder;
import com.jme3.ai.navmesh.Path.Waypoint;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
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

/**
 *
 * @author darylfed
 */
public class ControlCoche extends AbstractControl {

    private BetterCharacterControl playerControl;

    private Node playerNode; // Nodo donde estara el jugador en el mapa

    private Node playerNodes; // Nodo donde estan todos los jugadores incluido el

    private Vector3f finalPoint;
    private boolean moving = false;

    private NavMeshPathfinder navi;
    private NavMesh navMesh;

    private Geometry player; // Despues se reemplazara al implementar el modelo3d
    private Node bola; //TODO esto hay que instanciarlo

    private boolean shot = false;
    private boolean shooting = false;
    private float cooldownShot;
    private final float COOL_DOWN_SHOT = 1f; // un segundo 

    private boolean freezed = false;
    private float cooldownFreeze;
    private final float COOLDOWN_FREEZE = 2f; // dos segundos

    private AssetManager assetManager;
    private BulletAppState bulletAppState;
    private Node bulletNodes;

    private int healthPoints = 50;

    private int score = 0;

    private Vector3f lastDir;

    private boolean activateShooting = false;

    public ControlCoche(Node playerNode, BetterCharacterControl controler, NavMeshPathfinder navi, AssetManager assetsManager, BulletAppState bulletAppState, Node bulletNodes, Node playerNodes) {
        this.playerNode = playerNode;
        this.playerNodes = playerNodes;
        this.navi = navi;
        this.assetManager = assetsManager;
        this.bulletNodes = bulletNodes;
        this.bulletAppState = bulletAppState;

        spatial = playerNode;
        playerControl = controler;

    }

    public ControlCoche() {
        System.err.println("Empty ControlCcohe constructor has been called, we should avoid this");
    }

    @Override
    protected void controlUpdate(float tpf) {
        playerControl.setWalkDirection(Vector3f.ZERO);

        if (!freezed) {

            // Movimiento
            if (moving && finalPoint != null) {
                Waypoint wayPoint = navi.getNextWaypoint();

                if (wayPoint != null) {
                    Vector3f direccion = wayPoint.getPosition().subtract(playerNode.getLocalTranslation());
                    playerControl.setWalkDirection(direccion.normalize().mult(20));

                    // Settear direccion del coche
                    //Quaternion directionRot = new Quaternion();
                    //directionRot.lookAt(direccion.normalize(), Vector3f.UNIT_Y);
                    //playerNode.setLocalRotation(directionRot);
                    playerNode.lookAt(wayPoint.getPosition(), Vector3f.UNIT_Y);

                    if (playerNode.getLocalTranslation().distance(wayPoint.getPosition()) <= 4 && !navi.isAtGoalWaypoint()) {
                        System.out.println("Next waypoint");
                        navi.goToNextWaypoint();
                    }

                    if (navi.isAtGoalWaypoint()) {
                        playerNode.lookAt(playerNode.getLocalTranslation().add(lastDir).normalize().mult(20), Vector3f.UNIT_Y);
                        System.out.println("AT waypoint");
                        moving = false;
                        navi.clearPath();
                    }

                    lastDir = direccion;

                }

            }

            if (activateShooting) {
                // Busqueda del enemigo
                Vector3f dirEnemy = busquedaEnemy();

                // Disparo
                if (shot && !shooting && dirEnemy != null) {
                    shoot(dirEnemy);
                    shot = false;
                    shooting = true;
                    cooldownShot = COOL_DOWN_SHOT;
                } else if (cooldownShot > 0) {
                    cooldownShot -= tpf;
                    if (cooldownShot <= 0) {
                        shooting = false;
                        shot = false;
                    }
                }
            }

        } else {
            // Car is freezed, wait for defrost
            if (cooldownFreeze > 0) {
                cooldownFreeze -= tpf;
            } else {
                freezed = false;
            }
        }
    }

    private Vector3f busquedaEnemy() {
        Vector3f direccion = null;

        for (int i = 0; i < playerNodes.getQuantity(); ++i) {
            Spatial playerGetted = playerNodes.getChild(i);

            // Si no es el mismo
            if (!playerGetted.getName().equals(playerNode.getName())) {
                // SI estan bien los nodos en la escena no deberia de haber problemas con localTranslation
                float distancia = playerGetted.getLocalTranslation().distance(playerNode.getLocalTranslation());
                if (distancia < 4 * 3f) { // COmprobar distancia si esta bien
                    shot = true; // Shotear con una direcion, meter despues
                    direccion = playerGetted.getLocalTranslation().subtract(playerNode.getLocalTranslation()).normalize();
                }
                // Acabar bucle al encontrar enemigo
                break;
            }
        }

        return direccion;
    }

    public void computeNewPath(Vector3f finalPoint) {
        this.finalPoint = finalPoint;
        navi.setPosition(playerNode.getLocalTranslation());
        navi.computePath(finalPoint);
    }

    public void freeze() {
        freezed = true;
        cooldownFreeze = COOLDOWN_FREEZE;
    }

    private void shoot(Vector3f dirEnemy) {

        //Vector3f direccion = playerNode.getLocalRotation().getRotationColumn(2).normalize();
        Spatial ball = assetManager.loadModel("Models/ball.j3o");
        Vector3f direccion = dirEnemy.mult(2).add(0,2,0);
        //ball.setLocalTranslation(playerNode.getLocalTranslation().add(direccion.mult(3f)));
        ball.setLocalTranslation(playerNode.getLocalTranslation().add(direccion));
        ball.scale(0.25f);

        ball.setUserData("radius", 0.5f);

        RigidBodyControl ballControl = new RigidBodyControl(1.5f);
        ball.addControl(ballControl);
        bulletNodes.attachChild(ball);
        bulletAppState.getPhysicsSpace().add(ballControl);

        ball.setUserData("Control", ballControl);

        //ballControl.setLinearVelocity(direccion.mult(100));
        ballControl.setLinearVelocity(dirEnemy.mult(50));
        ball.setName(playerNode.getName());
    }

    public void makeHit() {
        healthPoints -= 10;
        System.out.println("Hitteado: " + healthPoints);
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

    public void setBola(Node bola) {
        this.bola = bola;
    }

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    public boolean isShot() {
        return shot;
    }

    public void setShot(boolean shot) {
        this.shot = shot;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isActivateShooting() {
        return activateShooting;
    }

    public void setActivateShooting(boolean activateShooting) {
        this.activateShooting = activateShooting;
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        //Only needed for rendering-related operations,
        //not called when spatial is culled.
    }

    public Control cloneForSpatial(Spatial spatial) {
        ControlCoche control = new ControlCoche();
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
