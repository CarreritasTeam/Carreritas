package mygame;

import com.jme3.ai.navmesh.NavMesh;
import com.jme3.ai.navmesh.NavMeshPathfinder;
import com.jme3.ai.navmesh.Path.Waypoint;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 *
 * @author normenhansen
 */
public class Main extends SimpleApplication implements ActionListener {

    private BulletAppState bulletAppState;

    private Vector3f targetVector = new Vector3f(30, 0, 9);

    private Bola bola;
    private Node bolaNode, playerNodes, bulletNodes;

    private int id = 0;

    private int selectedCar = 0;

    // Player Nodes contiene a todos los nodos jugadores
    // bulletNodes contiene a todos los nodos Bala
    private NavMesh navmesh;

    private RigidBodyControl landscapeControl;

    private boolean start = false;

    private float tiempo_espera = 0;
    private final float TIEMPO_COOLDOWN = 3f;

    private boolean mooving = false;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        bulletAppState.setDebugEnabled(false); // enable debug mode
        flyCam.setMoveSpeed(50f);

        initInput();
        initScene();

        Box b = new Box(1, 1, 1);
        Geometry geom = new Geometry("Box", b);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        geom.setMaterial(mat);

//        rootNode.attachChild(geom);
    }

    private void initInput() {
        inputManager.addMapping("Mouse", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("Space", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("ChangeCar", new KeyTrigger(KeyInput.KEY_C));
        inputManager.addMapping("MouseRight", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));

        inputManager.addListener(this, new String[]{"Mouse", "Space", "MouseRight", "ChangeCar"});
    }

    private void initScene() {
        //Spatial scene = assetManager.loadModel("Scenes/terreno.j3o");
        Spatial scene = assetManager.loadModel("Scenes/Plataforma.j3o");

        landscapeControl = new RigidBodyControl(0.0f);
        scene.addControl(landscapeControl);
        bulletAppState.getPhysicsSpace().add(landscapeControl);

        //scene.setLocalTranslation(0, -30, 0);
        landscapeControl.setPhysicsLocation(new Vector3f(0, -5, 0));
        bulletAppState.getPhysicsSpace().addAll(scene);
        rootNode.attachChild(scene);

        cam.setLocation(new Vector3f(0, 5, 10));

        // PlayerNodes y Bola Nodes instanciar
        playerNodes = new Node("Players");
        bulletNodes = new Node("Bullets");

        rootNode.attachChild(bulletNodes);
        rootNode.attachChild(playerNodes);

        /**
         * A white ambient light source.
         */
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White);
        rootNode.addLight(ambient);
        /**
         * A white, directional light source
         */
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection((new Vector3f(-0.5f, -0.5f, -0.5f)).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);

        iniciarsky();

        //playerNode = new Node("PlayerNode");
        Node n = (Node) scene;
        Geometry g = (Geometry) n.getChild("NavMesh");
        Mesh mesh = g.getMesh();

        navmesh = new NavMesh(mesh);

        crearBola();
        crearCoche(new Vector3f(0, 10, 0));
        crearCoche(new Vector3f(50, 10, 10));

    }

    private void iniciarsky() {
        Spatial sky = assetManager.loadModel("Scenes/Sky.j3o");
        rootNode.attachChild(sky);
    }

    // Crea un coche
    public void crearCoche(Vector3f pos) {

        Spatial player = assetManager.loadModel("Models/CocheSimple.j3o");
        Quaternion spatialGiro = new Quaternion();
        player.setLocalRotation(spatialGiro.fromAngleAxis(FastMath.DEG_TO_RAD * 180, Vector3f.UNIT_Y));
        player.scale(1.5f);
        //body.setPhysicsLocation(new Vector3f(0, 9, 0));
        //player.scale(0.05f, 0.05f, 0.05f);
        Node playerNode = new Node("PlayerNode");
        playerNode.attachChild(player);

        BetterCharacterControl playerControl = new BetterCharacterControl(1.5f, 3, 20);
        playerNode.addControl(playerControl);
        playerNode.setLocalTranslation(pos.x, 16, pos.z);
        //playerControl.setGravity(new Vector3f(0, -10, 0));
        //playerControl.setJumpForce(new Vector3f(0, 0, 0));
        playerControl.warp(new Vector3f(0, 2, 0));

        bulletAppState.getPhysicsSpace().add(playerControl);
        bulletAppState.getPhysicsSpace().addAll(playerNode);
        //bulletAppState.getPhysicsSpace().add(body);

        // Poner en el node de players
        playerNodes.attachChild(playerNode);
        //rootNode.attachChild(playerNode);

        NavMeshPathfinder navi = new NavMeshPathfinder(navmesh);

        ControlCoche control = new ControlCoche(playerNode, playerControl, navi, assetManager, bulletAppState, bulletNodes, playerNodes);
        playerNode.addControl(control);

        // Poner control y datos
        playerNode.setUserData("Control", control);
        playerNode.setUserData("radius", 3f); // Settear si el coche lo ponemos a otra escala
        playerNode.setName("Coche" + id);
        ++id; // para distincion de coches

    }

    public void crearBola() {
        Sphere c = new Sphere(5, 5, 0.5f);
        Geometry bolaGeom = new Geometry("Esfera", c);
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", new ColorRGBA(0.247f, 0.285f, 0.678f, 1));
        bolaGeom.setMaterial(material);
        bolaGeom.setLocalTranslation(0, 0, 0);//(0, 3, 0);
        //RigidBodyControl body = new RigidBodyControl(1f);
        //player.addControl(body);

        //body.setPhysicsLocation(new Vector3f(0, 9, 0));
        //player.scale(0.05f, 0.05f, 0.05f);
        bolaNode = new Node("Esfera");
        bolaNode.attachChild(bolaGeom);

        BetterCharacterControl bolaControl = new BetterCharacterControl(0.5f, 0.5f, 20);
        bolaNode.addControl(bolaControl);
        bolaControl.setGravity(new Vector3f(0, -10, 0));
        bolaControl.setJumpForce(new Vector3f(0, 30, 0));
        bolaControl.warp(new Vector3f(0, 2, 0));

        bulletAppState.getPhysicsSpace().add(bolaControl);
        bulletAppState.getPhysicsSpace().addAll(bolaNode);
        //bulletAppState.getPhysicsSpace().add(body);

        rootNode.attachChild(bolaNode);

        NavMeshPathfinder navi = new NavMeshPathfinder(navmesh);

        bola = new Bola(bolaNode, bolaControl, navi);
        bolaNode.addControl(bola);
        bolaNode.setUserData("radius", 0.5f); // Set user data para collision

    }

    private void handleCollisions() {
        // entre bullets y coche
        List<Spatial> listaBulletQuitar = new ArrayList<>();

//        System.out.println(playerNodes.getQuantity());
//        System.out.println(bulletNodes.getQuantity());
        // Deteccion collision coches
        for (int i = 0; i < playerNodes.getQuantity(); ++i) {
            Spatial playerGetted = playerNodes.getChild(i);
            for (int j = 0; j < bulletNodes.getQuantity(); ++j) {
                Spatial bulletGetted = bulletNodes.getChild(j);
                if (checkCollision(playerGetted, bulletGetted)) {

                    // Comprobar si e bullet no es del propio coche
                    if (!playerGetted.getName().equals(bulletGetted.getName())) {
                        // Ha colisionado, player perder vida, bullet quitar de lista despues de iterar
                        listaBulletQuitar.add(bulletGetted);

                        // ESTO NO DEBERIA DE PETAR
                        ControlCoche ctrl = playerGetted.getControl(ControlCoche.class);
                        ctrl.makeHit();
                    }

                }
            }

            // Collision con la bola
            // DEBERIA DE FUNCIONAR
            if (checkCollision(playerGetted, bolaNode)) {
                Bola ctrl = bolaNode.getControl(Bola.class);
                ctrl.onCollisionWithPlayer();
                ControlCoche ctrlCoche = playerGetted.getControl(ControlCoche.class);
                ctrlCoche.setScore(ctrlCoche.getScore() + 10);
            }
        }

        if (!listaBulletQuitar.isEmpty()) {
            for (Spatial bullet : listaBulletQuitar) {
                // NO DEBERIA DE PETAR lo de physics
                bulletAppState.getPhysicsSpace().remove(bullet.getControl(RigidBodyControl.class));
                bulletNodes.detachChild(bullet);
            }
        }

    }

    private boolean checkCollision(Spatial a, Spatial b) {
        float distance = a.getLocalTranslation().distance(b.getLocalTranslation());
        float maxDistance = (Float) a.getUserData("radius") + (Float) b.getUserData("radius");
        return distance <= maxDistance;
    }

    private void testShot() {
        Vector3f direccion = cam.getDirection().normalize();
        Spatial ball = assetManager.loadModel("Models/ball.j3o");
        ball.setLocalTranslation(cam.getLocation());
        ball.scale(0.5f);

        ball.setUserData("radius", 0.5f);

        RigidBodyControl ballControl = new RigidBodyControl(1.5f);
        ball.addControl(ballControl);
        bulletNodes.attachChild(ball);
        bulletAppState.getPhysicsSpace().add(ballControl);

        ball.setUserData("Control", ballControl);

        ballControl.setLinearVelocity(direccion.mult(100));

        ball.setName("Coche" + (id - 1));
    }

    @Override
    public void simpleUpdate(float tpf) {
        handleCollisions();

        if (start) {
            if (!mooving) {
                Vector3f posBola = bolaNode.getLocalTranslation();

                for (int i = 0; i < playerNodes.getQuantity(); ++i) {
                    Spatial playerGetted = playerNodes.getChild(i);
                    ControlCoche ctrl = playerGetted.getControl(ControlCoche.class);

                    ctrl.computeNewPath(posBola);
                    ctrl.setMoving(!ctrl.isMoving());
                    
                }
                mooving = true;
                tiempo_espera = TIEMPO_COOLDOWN;
            }
            tiempo_espera -= tpf;
            
            if(tiempo_espera <= 0){
                mooving = false;
            }
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("Mouse") && !isPressed) {
            CollisionResults results = new CollisionResults();
            Ray ray = new Ray(cam.getLocation(), cam.getDirection());

            rootNode.collideWith(ray, results);
            /*
            if (results.size() > 0) {
                targetVector = results.getClosestCollision().getContactPoint();
                ControlCoche control = playerNodes.getChild(selectedCar).getControl(ControlCoche.class);
                control.computeNewPath(targetVector);
                System.out.println("Vector pinchado = " + targetVector.toString());
            }
             */
            ControlCoche control = playerNodes.getChild(selectedCar).getControl(ControlCoche.class);
            control.computeNewPath(bolaNode.getLocalTranslation());

        }

        if (name.equals("Space") && isPressed) {
            // Settear a todos a moving
            //start = true;
            //ControlCoche control = playerNodes.getChild(selectedCar).getControl(ControlCoche.class);
            //control.setMoving(!control.isMoving());

            start = !start;
            for (int i = 0; i < playerNodes.getQuantity(); ++i) {
                Spatial playerGetted = playerNodes.getChild(i);
                ControlCoche ctrl = playerGetted.getControl(ControlCoche.class);
                ctrl.computeNewPath(bolaNode.getLocalTranslation());
                ctrl.setMoving(!ctrl.isMoving());
            }
            mooving = true;
            tiempo_espera = TIEMPO_COOLDOWN;
        }

        if (name.equals("MouseRight") && !isPressed) {
            //testShot();
            ControlCoche control = playerNodes.getChild(selectedCar).getControl(ControlCoche.class);
            control.setActivateShooting(!control.isActivateShooting());
            //control.setShot(true);
            //control.freeze();
        }

        if (name.equals("ChangeCar") && !isPressed) {
            selectedCar = (selectedCar + 1) % 2;
        }
    }
}
