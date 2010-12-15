import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
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
	
	public ArrayList<float[]> texCoords = new ArrayList<float[]>();
	public  ArrayList<Point3f> input_verts = new ArrayList<Point3f> ();
	public  ArrayList<Integer> input_faces = new ArrayList<Integer> ();
	public  ArrayList<Integer> face_tex = new ArrayList<Integer> ();

	public  ArrayList<Vector3f> input_norms = new ArrayList<Vector3f> ();
	public int texture;
	
	private Color3f col = null;
	
	public void setCol(Color3f c){
		col=c;
	}
	
	public boolean hasTexture = false;
	
	public void setTexture(String file, GL gl, GLU glu){
		
		gl.glEnable(GL.GL_TEXTURE_2D);
		//texture stuff
		//stolen from nehe java tutorial
		texture = genTexture(gl);
        gl.glBindTexture(GL.GL_TEXTURE_2D, texture);
        TextureReader.Texture texture = null;
        try {
            texture = TextureReader.readTexture(file);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        makeRGBTexture(gl, glu, texture, GL.GL_TEXTURE_2D, false);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        
        //hasTexture=true;
        gl.glDisable(GL.GL_TEXTURE_2D);
		
	}
	private void makeRGBTexture(GL gl, GLU glu, TextureReader.Texture img, 
            int target, boolean mipmapped) {
        
        if (mipmapped) {
            glu.gluBuild2DMipmaps(target, GL.GL_RGB8, img.getWidth(), 
                    img.getHeight(), GL.GL_RGB, GL.GL_UNSIGNED_BYTE, img.getPixels());
        } else {
            gl.glTexImage2D(target, 0, GL.GL_RGB, img.getWidth(), 
                    img.getHeight(), 0, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, img.getPixels());
        }
    }

    private int genTexture(GL gl) {
        final int[] tmp = new int[1];
        gl.glGenTextures(1, tmp, 0);
        return tmp[0];
    }
		

	public void Draw(GL gl) {
		
		if(hasTexture){
			gl.glColor3f(1, 1, 1);
			gl.glEnable(GL.GL_TEXTURE_2D);

			gl.glBindTexture(GL.GL_TEXTURE_2D, texture);
			//int numcoords = texCoords.size();
			
			int i;
			gl.glBegin(GL.GL_TRIANGLES);
			for (i = 0; i < input_faces.size(); i ++) {
				//int mycoord = (int)(numcoords * (float)i/(float)input_faces.size());
				//System.out.println(mycoord);
				int mycoord=face_tex.get(i);
				gl.glTexCoord2f(texCoords.get(mycoord)[0], texCoords.get(mycoord)[1]);
				
				int vid = input_faces.get(i);
				
				gl.glNormal3f(input_norms.get(i).x, input_norms.get(i).y, input_norms.get(i).z);
				gl.glVertex3f(input_verts.get(vid).x, input_verts.get(vid).y, input_verts.get(vid).z);
				
			}
			gl.glEnd();
			gl.glDisable(GL.GL_TEXTURE_2D);
			
		}else{
			
			//check
			//gl.glEnable( GL.GL_COLOR_MATERIAL ) ;
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
		
			//gl.glDisable( GL.GL_COLOR_MATERIAL ) ;
			 
		}
		
	}
	
	
	
	public R3dobject(String filename, boolean tex ) {
		hasTexture=tex;
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
		
		int t1, t2, t3;
		t1=t2=t3=-1;
		
		String line;
		String[] tokens;
		
		try {
		while ((line = in.readLine()) != null) {
			if (line.length() == 0)
				continue;
			switch(line.charAt(0)) {
			case 'v':
				if(line.charAt(1)=='t'){
					tokens = line.split("[ ]+");
					float[] coords = new float[2];
					coords[0]=Float.valueOf(tokens[1]);
					coords[1]=Float.valueOf(tokens[2]);
					texCoords.add(coords);
					break;
				}else if(line.charAt(1)=='n')
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
				if(p!=-1){
					//gogo texture coords!
					v1 = Integer.valueOf(tokens[1].substring(0,p))-1;
					int p2 = tokens[1].indexOf("/",p+1);
					t1 = Integer.valueOf(tokens[1].substring(p+1,p2))-1;
					//System.out.println(t1);
				}
				else
					v1 = Integer.valueOf(tokens[1])-1;

				p = tokens[2].indexOf("//");
				if(p==-1)
					p = tokens[2].indexOf("/");
				if(p!=-1){
					v2 = Integer.valueOf(tokens[2].substring(0,p))-1;
					int p2 = tokens[2].indexOf("/",p+1);
					t2 = Integer.valueOf(tokens[2].substring(p+1,p2))-1;
				}
				else
					v2 = Integer.valueOf(tokens[2])-1;
				
				p = tokens[3].indexOf("//");
				if(p==-1)
					p = tokens[3].indexOf("/");
				if(p!=-1){
					v3 = Integer.valueOf(tokens[3].substring(0,p))-1;
					int p2 = tokens[3].indexOf("/",p+1);
					t3 = Integer.valueOf(tokens[3].substring(p+1,p2))-1;
				}
				else
					v3 = Integer.valueOf(tokens[3])-1;
				
				input_faces.add(v1);
				input_faces.add(v2);
				input_faces.add(v3);	
				
				face_tex.add(t1);
				face_tex.add(t2);
				face_tex.add(t3);

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
		System.out.println("AND "+texCoords.size() + " tex coords for " + face_tex.size() + " faces.");
		
		
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
		
		if(!tex){
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
			
			
			
		}else{
		
			int i;
		
			input_norms.clear();
			for (i = 0; i < input_faces.size(); i ++) {
				input_norms.add(new Vector3f());
			}
			
			Vector3f e1 = new Vector3f();
			Vector3f e2 = new Vector3f();
			for (i = 0; i < input_faces.size()/3; i ++) {
				// get face
				 v1 = input_faces.get(3*i+0);
				 v2 = input_faces.get(3*i+1);
				 v3 = input_faces.get(3*i+2);
				
				// compute normal
				e1.sub(input_verts.get(v2), input_verts.get(v1));
				e2.sub(input_verts.get(v3), input_verts.get(v1));
				input_norms.get(i*3+0).cross(e1, e2);
				input_norms.get(i*3+0).normalize();
				input_norms.get(i*3+1).cross(e1, e2);
				input_norms.get(i*3+1).normalize();
				input_norms.get(i*3+2).cross(e1, e2);
				input_norms.get(i*3+2).normalize();
			}
		}
		
	}	
	
}



