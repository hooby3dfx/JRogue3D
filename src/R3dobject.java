import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Color3f;

import com.sun.opengl.util.BufferUtil;


public class R3dobject implements R3dBasicObject{

	public FloatBuffer vertexBuffer;
	public IntBuffer faceBuffer;
	public FloatBuffer normalBuffer;
	public Point3f center;
	public int num_verts;		// number of vertices
	public int num_faces;		// number of triangle faces
	
	private Color3f col = null;
	public void setCol(Color3f c){
		col=c;
	}
	
	//texture code here?
	

	public void Draw(GL gl) {
		vertexBuffer.rewind();
		normalBuffer.rewind();
		faceBuffer.rewind();
		
		if(col!=null)
			gl.glColor3f( col.x, col.y, col.z);
		
		gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
		
		gl.glVertexPointer(3, GL.GL_FLOAT, 0, vertexBuffer);
		gl.glNormalPointer(GL.GL_FLOAT, 0, normalBuffer);
		
		gl.glDrawElements(GL.GL_TRIANGLES, num_faces*3, GL.GL_UNSIGNED_INT, faceBuffer);
		
		gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
	}
	
	
	
	public R3dobject(String filename) {
		/* load a triangular mesh model from a .obj file */
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(filename));
		} catch (IOException e) {
			System.out.println("Error reading from file " + filename);
			System.exit(0);
		}

		center = new Point3f();			
		float x, y, z;
		int v1, v2, v3;
		float minx, miny, minz;
		float maxx, maxy, maxz;
		float bbx, bby, bbz;
		minx = miny = minz = 10000.f;
		maxx = maxy = maxz = -10000.f;
		
		String line;
		String[] tokens;
		ArrayList<Point3f> input_verts = new ArrayList<Point3f> ();
		ArrayList<Integer> input_faces = new ArrayList<Integer> ();
		ArrayList<Vector3f> input_norms = new ArrayList<Vector3f> ();
		try {
		while ((line = in.readLine()) != null) {
			if (line.length() == 0)
				continue;
			switch(line.charAt(0)) {
			case 'v':
				if(line.charAt(1)=='t'||line.charAt(1)=='n')
					continue;
				tokens = line.split("[ ]+");
				x = Float.valueOf(tokens[1]);
				y = Float.valueOf(tokens[2]);
				z = Float.valueOf(tokens[3]);
				minx = Math.min(minx, x);
				miny = Math.min(miny, y);
				minz = Math.min(minz, z);
				maxx = Math.max(maxx, x);
				maxy = Math.max(maxy, y);
				maxz = Math.max(maxz, z);
				input_verts.add(new Point3f(x, y, z));
				center.add(new Point3f(x, y, z));
				break;
			case 'f':
				tokens = line.split("[ ]+");
				
				int p = tokens[1].indexOf("//");
				if(p==-1)
					p = tokens[1].indexOf("/");
				if(p!=-1)
					v1 = Integer.valueOf(tokens[1].substring(0,p))-1;
				else
					v1 = Integer.valueOf(tokens[1])-1;

				p = tokens[2].indexOf("//");
				if(p==-1)
					p = tokens[2].indexOf("/");
				if(p!=-1)
					v2 = Integer.valueOf(tokens[2].substring(0,p))-1;
				else
					v2 = Integer.valueOf(tokens[2])-1;
				
				p = tokens[3].indexOf("//");
				if(p==-1)
					p = tokens[3].indexOf("/");
				if(p!=-1)
					v3 = Integer.valueOf(tokens[3].substring(0,p))-1;
				else
					v3 = Integer.valueOf(tokens[3])-1;
				
				input_faces.add(v1);
				input_faces.add(v2);
				input_faces.add(v3);				
				break;
			default:
				continue;
			}
		}
		in.close();	
		} catch(IOException e) {
			System.out.println("Unhandled error while reading input file.");
		}

		System.out.println("Read " + input_verts.size() +
					   	" vertices and " + input_faces.size() + " faces.");
		
		center.scale(1.f / (float) input_verts.size());
		
		bbx = maxx - minx;
		bby = maxy - miny;
		bbz = maxz - minz;
		float bbmax = Math.max(bbx, Math.max(bby, bbz));
		
		for (Point3f p : input_verts) {
			
			p.x = (p.x - center.x) / bbmax;
			p.y = (p.y - center.y) / bbmax;
			p.z = (p.z - center.z) / bbmax;
		}
		center.x = center.y = center.z = 0.f;
		
		/* estimate per vertex average normal */
		int i;
		for (i = 0; i < input_verts.size(); i ++) {
			input_norms.add(new Vector3f());
		}
		
		Vector3f e1 = new Vector3f();
		Vector3f e2 = new Vector3f();
		Vector3f tn = new Vector3f();
		for (i = 0; i < input_faces.size(); i += 3) {
			v1 = input_faces.get(i+0);
			v2 = input_faces.get(i+1);
			v3 = input_faces.get(i+2);
			
			e1.sub(input_verts.get(v2), input_verts.get(v1));
			e2.sub(input_verts.get(v3), input_verts.get(v1));
			tn.cross(e1, e2);
			input_norms.get(v1).add(tn);
			
			e1.sub(input_verts.get(v3), input_verts.get(v2));
			e2.sub(input_verts.get(v1), input_verts.get(v2));
			tn.cross(e1, e2);
			input_norms.get(v2).add(tn);
			
			e1.sub(input_verts.get(v1), input_verts.get(v3));
			e2.sub(input_verts.get(v2), input_verts.get(v3));
			tn.cross(e1, e2);
			input_norms.get(v3).add(tn);			
		}

		/* convert to buffers to improve display speed */
		for (i = 0; i < input_verts.size(); i ++) {
			input_norms.get(i).normalize();
		}
		
		vertexBuffer = BufferUtil.newFloatBuffer(input_verts.size()*3);
		normalBuffer = BufferUtil.newFloatBuffer(input_verts.size()*3);
		faceBuffer = BufferUtil.newIntBuffer(input_faces.size());
		
		for (i = 0; i < input_verts.size(); i ++) {
			vertexBuffer.put(input_verts.get(i).x);
			vertexBuffer.put(input_verts.get(i).y);
			vertexBuffer.put(input_verts.get(i).z);
			normalBuffer.put(input_norms.get(i).x);
			normalBuffer.put(input_norms.get(i).y);
			normalBuffer.put(input_norms.get(i).z);			
		}
		
		for (i = 0; i < input_faces.size(); i ++) {
			faceBuffer.put(input_faces.get(i));	
		}			
		num_verts = input_verts.size();
		num_faces = input_faces.size()/3;
	}		
}



