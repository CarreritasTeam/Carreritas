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
public class Main extends SimpleApplication implements ActionListener{

    private BulletAppState bulletAppState;
    private Node playerNode;
    private BetterCharacterControl playerControl;
    
    private NavMeshPathfinder navi;
    private Vector3f targetVector = new Vector3f(30, 0, 9);
    
    private boolean naviOn = false;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        bulletAppState.setDebugEnabled(true); // enable debug mode
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
        
        
        inputManager.addListener(this, new String[] {"Mouse", "Space"});
    }

    private void initScene() {
        Spatial scene = assetManager.loadModel("Models/terreno.j3o");
        scene.setLocalTranslation(0, 0, 0);
        bulletAppState.getPhysicsSpace().addAll(scene);
        rootNode.attachChild(scene);

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
        
        //Node player = (Node) assetManager.loadAsset("Models/Ninja/Ninja.mesh.xml");
        Box c = new Box(2f, 9f, 2f);
        Geometry player = new Geometry("Player", c);
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", new ColorRGBA(0.247f, 0.285f, 0.678f, 1));
        player.setMaterial(material);
        player.setLocalTranslation(0, 3, 0);
        //RigidBodyControl body = new RigidBodyControl(1f);
        //player.addControl(body);
        
        //body.setPhysicsLocation(new Vector3f(0, 9, 0));
        
        //player.scale(0.05f, 0.05f, 0.05f);
        playerNode = new Node("PlayerNode");
        playerNode.attachChild(player);
        
        playerControl = new BetterCharacterControl(1.5f, 9f, 15);
        playerNode.addControl(playerControl);
        playerControl.setGravity(new Vector3f(0, -10, 0));
        playerControl.setJumpForce(new Vector3f(0, 30, 0));
        playerControl.warp(new Vector3f(0, 2, 0));
        
        bulletAppState.getPhysicsSpace().add(playerControl);
        bulletAppState.getPhysicsSpace().addAll(playerNode);
        //bulletAppState.getPhysicsSpace().add(body);
        
        rootNode.attachChild(playerNode);
        
        Node n = (Node) scene;
        Geometry g = (Geometry) n.getChild("NavMesh");
        Mesh mesh = g.getMesh();
        NavMesh navmesh = new NavMesh(mesh);
        
        navi = new NavMeshPathfinder(navmesh);
        navi.setPosition(playerNode.getLocalTranslation());
        navi.computePath(targetVector);
    }

    @Override
    public void simpleUpdate(float tpf) {
        playerControl.setWalkDirection(Vector3f.ZERO);
        
        if(naviOn){
            Waypoint wayPoint = navi.getNextWaypoint();
            
            if(wayPoint == null){
                System.out.println("Waypoint null");
            }else{
                Vector3f v = wayPoint.getPosition().subtract(playerNode.getLocalTranslation());
                playerControl.setWalkDirection(v.normalize().mult(20));
                
                if(playerNode.getLocalTranslation().distance(wayPoint.getPosition()) <= 4 && !navi.isAtGoalWaypoint()){
                    System.out.println("Next waypoint");
                    navi.goToNextWaypoint();
                }
                
                if(navi.isAtGoalWaypoint()){
                    System.out.println("AT waypoint");
                    naviOn = false;
                    navi.clearPath();
                }
            }
            
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if(name.equals("Mouse") && !isPressed){
            CollisionResults results = new CollisionResults();
            Ray ray = new Ray(cam.getLocation(), cam.getDirection());
            
            rootNode.collideWith(ray, results);
            if(results.size() > 0){
                targetVector = results.getClosestCollision().getContactPoint();
                navi.setPosition(playerNode.getLocalTranslation());
                navi.computePath(targetVector);
            }
        }
        
        if(name.equals("Space") && isPressed){
            naviOn = !naviOn;
        }
    }
}
