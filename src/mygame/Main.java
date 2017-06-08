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

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 *
 * @author normenhansen
 */
public class Main extends SimpleApplication implements ActionListener {

    private BulletAppState bulletAppState;

    private Vector3f targetVector = new Vector3f(30, 0, 9);

    private ControlCoche control;
    private Node playerNode;
    
    private NavMesh navmesh;

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

        rootNode.attachChild(geom);
    }

    private void initInput() {
        inputManager.addMapping("Mouse", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("Space", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("MouseRight", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));

        inputManager.addListener(this, new String[]{"Mouse", "Space", "MouseRight"});
    }

    private void initScene() {
        Spatial scene = assetManager.loadModel("Scenes/terreno.j3o");
        scene.setLocalTranslation(0, 0, 0);
        bulletAppState.getPhysicsSpace().addAll(scene);
        rootNode.attachChild(scene);

        cam.setLocation(new Vector3f(0, 5, 10));
        
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

        playerNode = new Node("PlayerNode");

        Node n = (Node) scene;
        Geometry g = (Geometry) n.getChild("NavMesh");
        Mesh mesh = g.getMesh();
        
        navmesh = new NavMesh(mesh);

        crearCoche();

    }

    // Crea un coche
    public void crearCoche() {
        //Box c = new Box(2f, 9f, 2f);
        //Geometry player = new Geometry("Player", c);
        //Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        //material.setColor("Color", new ColorRGBA(0.247f, 0.285f, 0.678f, 1));
        //player.setMaterial(material);
       //player.setLocalTranslation(0, 0, 0);
        //RigidBodyControl body = new RigidBodyControl(1f);
        //player.addControl(body);
        
        Spatial player = assetManager.loadModel("Models/CocheSimple.j3o");
        Quaternion spatialGiro = new Quaternion();
        player.setLocalRotation(spatialGiro.fromAngleAxis(FastMath.DEG_TO_RAD * 180, Vector3f.UNIT_Y));
        player.scale(1.5f);
        //body.setPhysicsLocation(new Vector3f(0, 9, 0));
        //player.scale(0.05f, 0.05f, 0.05f);
        playerNode = new Node("PlayerNode");
        playerNode.attachChild(player);

        BetterCharacterControl playerControl = new BetterCharacterControl(1.5f, 9f, 20);
        playerNode.addControl(playerControl);
        playerNode.setLocalTranslation(0, 16, 0);
        playerControl.setGravity(new Vector3f(0, -10, 0));
        playerControl.setJumpForce(new Vector3f(0, 30, 0));
        playerControl.warp(new Vector3f(0, 2, 0));

        bulletAppState.getPhysicsSpace().add(playerControl);
        bulletAppState.getPhysicsSpace().addAll(playerNode);
        //bulletAppState.getPhysicsSpace().add(body);

        rootNode.attachChild(playerNode);
        
        NavMeshPathfinder navi = new NavMeshPathfinder(navmesh);
        
        control = new ControlCoche(playerNode, playerControl, navi, assetManager, bulletAppState, rootNode);
        playerNode.addControl(control);

    }

    @Override
    public void simpleUpdate(float tpf) {
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
            if (results.size() > 0) {
                targetVector = results.getClosestCollision().getContactPoint();
                control.computeNewPath(targetVector);
            }
        }

        if (name.equals("Space") && isPressed) {
            control.setMoving(!control.isMoving());
        }
        
        if(name.equals("MouseRight") && !isPressed){
            //control.setShot(true);
            control.freeze();
        }
    }
}
