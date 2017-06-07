/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

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
    /*
    Se asume que target es un spatial que tiene como userData:
    "RigidBody" -> Obtiene su rigidBody
    "Lenght" -> longitud del spatial
    "Depth" -> profundidad del spatial
    */
    
    private float maxDist = 2.5f; // Distancia maxima a la que puede estar cerca de un objeto
    private RigidBodyControl body;
    private float lenght;
    private float depth;
    
    private Node collidables;
    
    public ControlCoche(Spatial target, Node collidables){
        this.collidables = collidables;
        this.spatial = target;
        lenght = target.getUserData("Lenght");
        depth = target.getUserData("Depth");
        body = spatial.getUserData("RigidBody");
    }

    @Override
    protected void controlUpdate(float tpf) {
        // Obtener direccion y posiciom del spatial
        // Raycast izquierda -> corregir
        // Raycast derecha -> corregir
        // Aplicar velocidad hacia delante
        Vector3f direccion = body.getPhysicsRotation().getRotationColumn(2);
        Vector3f posicion = body.getPhysicsLocation();
        Vector3f direccionLeft = direccion.cross(Vector3f.UNIT_Y).normalize();
        Vector3f direccionRight = direccionLeft.negate();
        
        // Raycast izquierda
        Vector3f offset = new Vector3f(direccionLeft.x > 0 ? lenght*0.5f : lenght*-0.5f,
        0, direccionLeft.z > 0 ? depth*0.5f : depth*-0.5f);
        boolean giroIzq = isNear(posicion.add(offset), direccionLeft);
        
        // Raycast derecha
        boolean giroDer = isNear(posicion.add(offset), direccionRight);
        
        // Aplicar giros
        if(giroIzq){
            body.setAngularVelocity(new Vector3f(0, 1, 0));
        }
        
        if(giroDer){
            body.setAngularVelocity(new Vector3f(0, -1, 0));
        }
        
        // Aplicar velocidad adelante
        
        
    }
    
    public boolean isNear(Vector3f position ,Vector3f direction){
        CollisionResults results = new CollisionResults();
        Ray raycast = new Ray(position, direction);
        raycast.collideWith(collidables, results);
        
        boolean isNear = false;
        
        for(int i = 0; i < results.size(); ++i){
            Geometry geom = results.getCollision(i).getGeometry();
            if(!geom.getName().equals(spatial.getName())){
                float distance = results.getCollision(i).getDistance();
                if(distance <= maxDist){
                    // aplicar giro
                    isNear = true;
                    break;
                }
            }
        }
        
        return isNear;
    }
    
    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        //Only needed for rendering-related operations,
        //not called when spatial is culled.
    }
    
    public Control cloneForSpatial(Spatial spatial) {
        ControlCoche control = new ControlCoche(spatial, collidables);
        //TODO: copy parameters to new Control
        return control;
    }
    
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        //TODO: load properties of this Control, e.g.
        //this.value = in.readFloat("name", defaultValue);
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        //TODO: save properties of this Control, e.g.
        //out.write(this.value, "name", defaultValue);
    }
    
}
