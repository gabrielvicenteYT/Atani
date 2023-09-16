package tech.atani.client.utility.render.shader.access;

import cn.muyang.nativeobfuscator.Native;
import tech.atani.client.utility.render.shader.shaders.BloomShader;
import tech.atani.client.utility.render.shader.shaders.BlurShader;
@Native
public interface ShaderAccess {

    BlurShader blurShader = new BlurShader();

    BloomShader bloomShader = new BloomShader();


}
