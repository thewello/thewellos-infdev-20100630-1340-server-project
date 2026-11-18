import org.lwjgl.opengl.GL11;

/** Draws other players with the normal player model and a name tag above them. */
public class TWPlayerRender extends dx {

    public void a(gi e, double x, double y, double z, float yaw, float partial) {
        super.a(e, x, y, z, yaw, partial);
        if (e instanceof TWRemotePlayer) {
            TWRemotePlayer rp = (TWRemotePlayer) e;
            double dist = rp.e(this.a.g);
            if (dist < 64.0 * 64.0) {
                renderLabel(rp.name, x, y - rp.z + 2.25, z);
            }
        }
    }

    private void renderLabel(String text, double x, double y, double z) {
        ej font = this.a.o;
        if (font == null) {
            return;
        }
        float scale = 0.016666668F * 1.6F;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-this.a.h, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(this.a.i, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-scale, -scale, scale);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        int w = font.a(text) / 2;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.25F);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(-w - 1, -1, 0.0F);
        GL11.glVertex3f(-w - 1, 8, 0.0F);
        GL11.glVertex3f(w + 1, 8, 0.0F);
        GL11.glVertex3f(w + 1, -1, 0.0F);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        font.b(text, -w, 0, 0x20FFFFFF);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        font.b(text, -w, 0, 0xFFFFFFFF);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }
}
