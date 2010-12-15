import javax.media.opengl.GL;

import com.sun.opengl.util.GLUT;
import javax.vecmath.Color3f;

//class for using glut shapes
//borrowed from warmup

public class R3dglobj implements R3dBasicObject {
	
	private final GLUT glut = new GLUT();
	

	public R3dglobj(){

		
	}
	private Color3f col = null;
	public void setCol(Color3f c){
		col=c;
	}
	
	//maybe use a shape for arrow, etc?
	// a set of shapes
	public static final int Triangle = 0, Torus = 1, Sphere = 2, 
		Icosahedron = 3, Teapot = 4, Cone = 5, Tetrahedron = 6;
	// initial shape is a triangle
	public int shape = Sphere;
	public float size=.25f;
	
	// draw the current shape
	
	public void Draw(GL gl) {
		gl.glColor3f(col.x, col.y, col.z);
		
		switch(shape) {
		case Triangle:
			gl.glBegin(GL.GL_TRIANGLES);
			gl.glVertex3f(0.0f, 1.0f, 0.0f);
			gl.glVertex3f(-1.0f, -0.5f, 0.0f);
			gl.glVertex3f(1.0f, -0.5f, 0.0f);
			gl.glEnd();
			break;
		case Torus:
			glut.glutSolidTorus(size, size*3, 32, 32);
			break;
		case Sphere:
			glut.glutSolidSphere(size, 32, 32);
			break;
		case Icosahedron:
			glut.glutSolidIcosahedron();
			break;
		case Teapot:
			gl.glFrontFace(GL.GL_CW);
			glut.glutSolidTeapot(1.0f);
			gl.glFrontFace(GL.GL_CCW);
			break;
		case Cone:
			glut.glutSolidCone(size, size*2, 32, 32);
			break;
		case Tetrahedron:
			glut.glutSolidTetrahedron();
			break;
			
		}
		
	}


}
