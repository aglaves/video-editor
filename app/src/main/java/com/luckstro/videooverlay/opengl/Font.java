package com.luckstro.videooverlay.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.luckstro.videooverlay.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by ejdd5cj on 7/3/2017.
 */

public class Font {
    // Our matrices
    private final float[] mtrxProjection = new float[16];
    private final float[] mtrxView = new float[16];
    private final float[] mtrxProjectionAndView = new float[16];

    public static short indices[] = new short[] {0, 1, 2, 0, 2, 3};
    public FloatBuffer vertexBuffer;
    public ShortBuffer drawListBuffer;
    public FloatBuffer uvBuffer;
    private FloatBuffer colorBuffer;

    public static float uvs[] = new float[] {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };

    float[] colors = new float[]
            {1f, 0f, 0f, 1f};

    // Our screenresolution (Or size of the view we are playing the video)
    private float viewWidth;
    private float viewHeight;

    // Misc
    private Context context;
    private int programId;
    private int textureId;

    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX = 3;
    private float triangleCoords[];

    public Font(Context context, float screenWidth, float screenHeight) {
        this.context = context;
        this.viewWidth = screenWidth;
        this.viewHeight = screenHeight;
    }

    public void create() {
        createTriangleCoordinates();
        createRectangle();
        setupImage();
        setupOpenGl();
        surfaceChanged(new Float(viewWidth).intValue(), new Float(viewHeight).intValue());
    }

    private void createTriangleCoordinates() {
        float leftX = 0;
        float rightX = viewWidth;
        float upperY = viewHeight/2;
        float lowerY = 0;

        triangleCoords = new float[]{
                leftX, upperY, 0f,
                leftX, lowerY, 0f,
                rightX, lowerY, 0f,
                rightX, upperY, 0f,
        };
    }

    private void createRectangle() {
        // The vertex buffer.
        ByteBuffer bb = ByteBuffer.allocateDirect(triangleCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(triangleCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);

        // The Color buffer.
        ByteBuffer bb3 = ByteBuffer.allocateDirect(colors.length * 4);
        bb3.order(ByteOrder.nativeOrder());
        colorBuffer = bb3.asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);
        TextureRender.checkGlError("TextManager colorBuffer");
    }

    private void setupOpenGl() {
        // Create the shaders, images
        int vertexShader = com.luckstro.videooverlay.RiGraphicTools.loadShader(GLES20.GL_VERTEX_SHADER,
                RiGraphicTools.vs_Text);
        int fragmentShader = com.luckstro.videooverlay.RiGraphicTools.loadShader(GLES20.GL_FRAGMENT_SHADER,
                RiGraphicTools.fs_Text);

        programId = GLES20.glCreateProgram();
        GLES20.glAttachShader(programId, vertexShader);
        GLES20.glAttachShader(programId, fragmentShader);
        GLES20.glLinkProgram(programId);
    }

    public void surfaceChanged(int width, int height) {
        // Clear our matrices
        for(int i=0;i<16;i++) {
            mtrxProjection[i] = 0.0f;
            mtrxView[i] = 0.0f;
            mtrxProjectionAndView[i] = 0.0f;
        }

        // Setup our screen width and height for normal sprite translation.
        Matrix.orthoM(mtrxProjection, 0, 0f, viewWidth, 0.0f, viewHeight, 0, 50);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);
    }

    public void draw() {
        // Set our shader program
        GLES20.glUseProgram(programId);

        // clear Screen and Depth Buffer, we have set the clear color as black.
        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        // get handle to vertex shader's vPosition member
        int positionHandle = GLES20.glGetAttribLocation(programId, "vPosition");

        // Enable generic vertex attribute array
        GLES20.glEnableVertexAttribArray(positionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(positionHandle, 3,
                GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // Get handle to texture coordinates location
        int mTexCoordLoc = GLES20.glGetAttribLocation(programId, "a_texCoord");

        // Enable generic vertex attribute array
        GLES20.glEnableVertexAttribArray ( mTexCoordLoc );

        // Prepare the texturecoordinates
        GLES20.glVertexAttribPointer ( mTexCoordLoc, 2, GLES20.GL_FLOAT,
                false,
                0, uvBuffer);

        int mColorHandle = GLES20.glGetAttribLocation(programId, "a_Color");
		TextureRender.checkGlError("TextManager a_Color");

	    // Enable a handle to the triangle vertices
	    GLES20.glEnableVertexAttribArray(mColorHandle);

	    // Prepare the background coordinate data
	    GLES20.glVertexAttribPointer(mColorHandle, 4,
	                                 GLES20.GL_FLOAT, false,
	                                 0, colorBuffer);
		TextureRender.checkGlError("TextManager enable mColorHandle");

        // Get handle to shape's transformation matrix
        int mtrxhandle = GLES20.glGetUniformLocation(programId, "uMVPMatrix");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, mtrxProjectionAndView, 0);

        // Get handle to textures locations
        int mSamplerLoc = GLES20.glGetUniformLocation (programId, "s_texture" );

        // Set the sampler texture unit to 0, where we have saved the texture.
        GLES20.glUniform1i ( mSamplerLoc, 0);

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordLoc);
        GLES20.glDisableVertexAttribArray(mColorHandle);
    }

    private void setupImage() {
        // The texture buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(uvs.length * 4);
        bb.order(ByteOrder.nativeOrder());
        uvBuffer = bb.asFloatBuffer();
        uvBuffer.put(uvs);
        uvBuffer.position(0);

        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.font);
        // Bind texture to texturename
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        // Set filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);

        // We are done using the bitmap so we should recycle it.
        bmp.recycle();
    }

    public void setTextureId(int textureId) {
        this.textureId = textureId;
    }
}
