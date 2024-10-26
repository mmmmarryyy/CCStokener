package eu.cherrytree.paj.graphics;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import javax.media.opengl.GL2;
import eu.cherrytree.paj.graphics.Graphics.ShaderProgramProcessException;
import eu.cherrytree.paj.graphics.light.Light;
import eu.cherrytree.paj.graphics.light.LightManager;
import eu.cherrytree.paj.gui.Console;

public class EnvironmentManager {

    public static final class ShaderIncludesNotFoundException extends Exception {

        private static final long serialVersionUID = -6539890206197395621L;

        public ShaderIncludesNotFoundException() {
            super("Couldn't read in shader include. Library jar may be corrupt.");
        }
    }

    private static String omniLightInclude;

    private static String spotLightInclude;

    private static String sunLightInclude;

    private static String torchLightInclude;

    private static String fogInclude;

    private static String normalMapInclude;

    public EnvironmentManager() throws ShaderIncludesNotFoundException {
        try {
            omniLightInclude = getShaderIncludeSource("/eu/cherrytree/paj/graphics/shaders/includes/omni.inc");
            spotLightInclude = getShaderIncludeSource("/eu/cherrytree/paj/graphics/shaders/includes/spot.inc");
            sunLightInclude = getShaderIncludeSource("/eu/cherrytree/paj/graphics/shaders/includes/sun.inc");
            torchLightInclude = getShaderIncludeSource("/eu/cherrytree/paj/graphics/shaders/includes/torch.inc");
            fogInclude = getShaderIncludeSource("/eu/cherrytree/paj/graphics/shaders/includes/fog.inc");
            normalMapInclude = getShaderIncludeSource("/eu/cherrytree/paj/graphics/shaders/includes/normal.inc");
        } catch (Exception e) {
            throw new ShaderIncludesNotFoundException();
        }
    }

    private String getShaderIncludeSource(String path) throws Exception {
        URL url = this.getClass().getResource(path);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        boolean run = true;
        String str;
        String ret = new String();
        while (run) {
            str = in.readLine();
            if (str != null) ret += str + "\n"; else run = false;
        }
        in.close();
        return ret;
    }

    public static boolean isShaderSupported(ShaderID shaderID) {
        int st_count = getNumberOfTextureCoordinates(shaderID.getTexturing(), shaderID.getTextureMapping(), shaderID.getLightMapping(), shaderID.getNormalMapping());
        int samp_count = getNumberOfSamplers(shaderID.getTexturing(), shaderID.getLightMapping(), shaderID.getNormalMapping());
        int max_st, max_samp;
        int[] tmp = new int[1];
        Graphics.getGL().glGetIntegerv(GL2.GL_MAX_TEXTURE_COORDS, tmp, 0);
        max_st = tmp[0];
        Graphics.getGL().glGetIntegerv(GL2.GL_MAX_TEXTURE_IMAGE_UNITS, tmp, 0);
        max_samp = tmp[0];
        return st_count <= max_st && samp_count <= max_samp;
    }

    public static void requestShader(Light.LightType[] lightType, boolean fog) {
        lightType = LightManager.getProperLightOrder(lightType);
        String shd_msg = ShaderBank.getBankName(lightType, fog);
        if (Graphics.getShaderBank(lightType, fog) != null) {
            Console.print(shd_msg + ", is already created.");
            return;
        }
        ShaderBank bank = new ShaderBank(lightType, fog);
        for (int i = 0; i < ShaderID.Texturing.values().length; i++) {
            for (int k = 0; k < ShaderID.NormalMap.values().length; k++) {
                for (int j = 0; j < ShaderID.Lighting.values().length; j++) {
                    for (int l = 0; l < ShaderID.TextureMaping.values().length; l++) {
                        ShaderID.Texturing et = ShaderID.Texturing.values()[i];
                        ShaderID.NormalMap enm = ShaderID.NormalMap.values()[k];
                        ShaderID.Lighting lnm = ShaderID.Lighting.values()[j];
                        ShaderID.TextureMaping tnm = ShaderID.TextureMaping.values()[l];
                        String v_src, f_src;
                        int textures;
                        v_src = generateVertexShaderSource(lightType, fog, et, tnm, lnm, enm);
                        f_src = generateFragmentShaderSource(lightType, fog, et, tnm, lnm, enm);
                        textures = getNumberOfSamplers(et, lnm, enm);
                        ShaderID sid = new ShaderID(et, enm, tnm, lnm);
                        if (isShaderSupported(sid)) {
                            try {
                                bank.addShader(sid, v_src, f_src, textures);
                            } catch (ShaderProgramProcessException e) {
                                System.err.println("Shader " + sid + " could not be loaded. " + e.getMessage());
                            }
                        } else System.err.println("Shader " + sid + " is not supported by the system.");
                    }
                }
            }
        }
        Graphics.addShaderBank(bank);
        Console.print(shd_msg + " created.");
    }

    private static int getNumberOfTextureCoordinates(ShaderID.Texturing texturing, ShaderID.TextureMaping texturemapping, ShaderID.Lighting lighting, ShaderID.NormalMap normalmap) {
        int textureamount = 0;
        boolean splat = false;
        boolean multiUV = (texturemapping == ShaderID.TextureMaping.MULTI_ST);
        switch(texturing) {
            case NONE:
                textureamount = 0;
                break;
            case SINGLE_TEXTURE:
                textureamount = 1;
                break;
            case GLOWING_TEXTURE:
                textureamount = 1;
                break;
            case SINGLE_TEXTURE_AND_GLOWING_TEXTURE:
                if (multiUV) textureamount = 2; else textureamount = 1;
                break;
            case SPLAT_MAP_1:
                if (multiUV) textureamount = 3; else textureamount = 2;
                splat = true;
                break;
            case SPLAT_MAP_2:
                if (multiUV) textureamount = 4; else textureamount = 2;
                splat = true;
                break;
            case SPLAT_MAP_3:
                if (multiUV) textureamount = 5; else textureamount = 2;
                splat = true;
                break;
            case SPLAT_MAP_4:
                if (multiUV) textureamount = 6; else textureamount = 2;
                splat = true;
                break;
        }
        if (normalmap != ShaderID.NormalMap.NONE && texturing == ShaderID.Texturing.NONE) textureamount = 1; else if (normalmap != ShaderID.NormalMap.NONE && texturing == ShaderID.Texturing.SINGLE_TEXTURE_AND_GLOWING_TEXTURE) {
            if (multiUV) textureamount = 3;
        } else if (normalmap == ShaderID.NormalMap.SEPARATE_UV_NORMALMAP) {
            if (multiUV) {
                textureamount *= 2;
                if (splat) textureamount--;
            }
        }
        if (lighting == ShaderID.Lighting.LIGHTMAP) textureamount++;
        return textureamount;
    }

    private static int getNumberOfSamplers(ShaderID.Texturing texturing, ShaderID.Lighting lighting, ShaderID.NormalMap normalmap) {
        int textureamount = 0;
        boolean splat = false;
        switch(texturing) {
            case NONE:
                textureamount = 0;
                break;
            case SINGLE_TEXTURE:
                textureamount = 1;
                break;
            case GLOWING_TEXTURE:
                textureamount = 1;
                break;
            case SINGLE_TEXTURE_AND_GLOWING_TEXTURE:
                textureamount = 2;
                break;
            case SPLAT_MAP_1:
                textureamount = 3;
                splat = true;
                break;
            case SPLAT_MAP_2:
                textureamount = 4;
                splat = true;
                break;
            case SPLAT_MAP_3:
                textureamount = 5;
                splat = true;
                break;
            case SPLAT_MAP_4:
                textureamount = 6;
                splat = true;
                break;
        }
        if (normalmap != ShaderID.NormalMap.NONE) {
            if (texturing == ShaderID.Texturing.NONE) textureamount = 1; else textureamount *= 2;
            if (splat) textureamount--;
        }
        if (lighting == ShaderID.Lighting.LIGHTMAP) textureamount++;
        return textureamount;
    }

    private static String generateVertexShaderSource(Light.LightType[] lights, boolean fog, ShaderID.Texturing texturing, ShaderID.TextureMaping texturemapping, ShaderID.Lighting lighting, ShaderID.NormalMap normalmap) {
        String out = new String();
        out += "varying vec3 v_V;\n";
        if (lights != null) {
            out += "varying vec3 v_N;\n";
            out += "\n";
        }
        if (fog) {
            out += "varying float depth;\n";
            out += "\n";
        }
        out += "uniform mat4 viewMat;\n";
        out += "uniform mat4 modelMat;\n";
        out += "\n";
        out += "void main()\n";
        out += "{\n";
        out += "	gl_Position = gl_ProjectionMatrix * viewMat * modelMat * gl_Vertex;\n";
        out += "	\n";
        if (fog) {
            out += "	depth = gl_Position.z;\n";
            out += "\n";
        }
        out += "	v_V = (modelMat * gl_Vertex).xyz;\n";
        if (lights != null) {
            out += "	v_N = normalize((modelMat * vec4(gl_Normal,0.0))).xyz;\n";
            out += "\n";
        }
        int textureamount = getNumberOfTextureCoordinates(texturing, texturemapping, lighting, normalmap);
        for (int i = 0; i < textureamount; i++) out += "	gl_TexCoord[" + i + "] = gl_MultiTexCoord" + i + ";\n";
        out += "\n";
        out += "}\n";
        return out;
    }

    private static String generateFragmentShaderSource(Light.LightType[] lights, boolean fog, ShaderID.Texturing texturing, ShaderID.TextureMaping texturemapping, ShaderID.Lighting lighting, ShaderID.NormalMap normalmap) {
        boolean multiUV = (texturemapping == ShaderID.TextureMaping.MULTI_ST);
        String out = new String();
        out += "varying vec3 v_V;\n";
        if (lighting == ShaderID.Lighting.NONE) lights = null;
        if (lights != null) {
            out += "varying vec3 v_N;\n";
            out += "\n";
        }
        if (fog) {
            out += "varying float depth;\n";
            out += "uniform float fogVerticalDisperse;\n";
            out += "\n";
        }
        if (lights != null) {
            out += "vec3 N;\n";
            out += "vec3 R;\n";
            out += "\n";
        }
        String lightString = new String();
        boolean addomni = false;
        boolean addtorch = false;
        boolean addsun = false;
        boolean addspot = false;
        if (lights != null) {
            for (int i = 0; i < lights.length; i++) {
                if (lights[i] != null) {
                    switch(lights[i]) {
                        case OMNILIGHT:
                            lightString += "		color += omni(gl_LightSource[" + i + "]);\n";
                            addomni = true;
                            break;
                        case SPOTLIGHT:
                            lightString += "		color += spot(gl_LightSource[" + i + "]);\n";
                            addspot = true;
                            break;
                        case SUN:
                            lightString += "		color += sun(gl_LightSource[" + i + "]);\n";
                            addsun = true;
                            break;
                        case TORCHLIGHT:
                            lightString += "		color += torch(gl_LightSource[" + i + "]);\n";
                            addtorch = true;
                            break;
                    }
                }
            }
        }
        int textureamount = getNumberOfSamplers(texturing, lighting, normalmap);
        for (int i = 0; i < textureamount; i++) out += "uniform sampler2D tex" + i + ";\n";
        out += "\n";
        if (addomni) out += omniLightInclude + "\n";
        if (addspot) out += spotLightInclude + "\n";
        if (addsun) out += sunLightInclude + "\n";
        if (addtorch) out += torchLightInclude + "\n";
        if (fog) out += fogInclude + "\n";
        if (normalmap != ShaderID.NormalMap.NONE) out += normalMapInclude + "\n";
        int texelamount = 0;
        int normalamount = 0;
        boolean splat = false;
        switch(texturing) {
            case NONE:
                texelamount = 0;
                break;
            case SINGLE_TEXTURE:
                texelamount = 1;
                break;
            case GLOWING_TEXTURE:
                texelamount = 1;
                break;
            case SINGLE_TEXTURE_AND_GLOWING_TEXTURE:
                texelamount = 2;
                break;
            case SPLAT_MAP_1:
                texelamount = 2;
                splat = true;
                break;
            case SPLAT_MAP_2:
                texelamount = 3;
                splat = true;
                break;
            case SPLAT_MAP_3:
                texelamount = 4;
                splat = true;
                break;
            case SPLAT_MAP_4:
                texelamount = 5;
                splat = true;
                break;
        }
        if (normalmap != ShaderID.NormalMap.NONE) {
            if (texturing == ShaderID.Texturing.NONE || texturing == ShaderID.Texturing.SINGLE_TEXTURE_AND_GLOWING_TEXTURE) normalamount = 1; else normalamount = texelamount;
        }
        out += "void main()\n";
        out += "{\n";
        int samplerno = 0;
        int uvno = 0;
        for (int i = 0; i < texelamount; i++, samplerno++) {
            out += "	vec4 texel" + i + " = texture2D(tex" + samplerno + ",gl_TexCoord[" + uvno + "].st);\n";
            if (multiUV) uvno++;
        }
        out += "\n";
        if (normalmap != ShaderID.NormalMap.SEPARATE_UV_NORMALMAP) uvno = 0;
        for (int i = 0; i < normalamount; i++, samplerno++) {
            out += "	vec3 normal_map" + i + " = normalize(2.0 * texture2D(tex" + samplerno + ",gl_TexCoord[" + uvno + "].st) - 1.0).xyz;\n";
            if (multiUV) uvno++;
        }
        out += "\n";
        if (!multiUV) uvno++;
        if (splat) {
            out += "	vec4 map = texture2D(tex" + samplerno + ",gl_TexCoord[" + uvno + "].st);\n\n";
            uvno++;
            samplerno++;
        }
        if (lighting == ShaderID.Lighting.LIGHTMAP) out += "	vec4 light_map = texture2D(tex" + samplerno + ",gl_TexCoord[" + uvno + "].st);\n";
        out += "\n";
        if (texelamount > 0) {
            out += "	if(";
            for (int i = 0; i < texelamount; i++) {
                out += "texel" + i + ".a == 0.0";
                if (i != texelamount - 1) out += " && "; else out += ")\n";
            }
            out += "		discard;\n";
            out += "	else\n";
        }
        out += "	{\n";
        switch(texturing) {
            case SPLAT_MAP_1:
                out += "		float base_v = 1.0 - map.r;\n\n";
                break;
            case SPLAT_MAP_2:
                out += "		float base_v = 1.0 - (map.r + map.g);\n\n";
                break;
            case SPLAT_MAP_3:
                out += "		float base_v = 1.0 - (map.r + map.g + map.b);\n\n";
                break;
            case SPLAT_MAP_4:
                out += "		float base_v = 1.0 - (map.r + map.g + map.b + map.a);\n\n";
                break;
        }
        if (normalmap != ShaderID.NormalMap.NONE) {
            switch(texturing) {
                case SINGLE_TEXTURE:
                case GLOWING_TEXTURE:
                case SINGLE_TEXTURE_AND_GLOWING_TEXTURE:
                case NONE:
                    out += "		vec3 normal_map = normal_map0;\n";
                    break;
                case SPLAT_MAP_1:
                    out += "		vec3 normal_map = normal_map0 * base_v + normal_map1 * map.r;\n";
                    break;
                case SPLAT_MAP_2:
                    out += "		vec3 normal_map = normal_map0 * base_v + normal_map1 * map.r + normal_map2 * map.g;\n";
                    break;
                case SPLAT_MAP_3:
                    out += "		vec3 normal_map = normal_map0 * base_v + normal_map1 * map.r + normal_map2 * map.g + normal_map3 * map.b;\n";
                    break;
                case SPLAT_MAP_4:
                    out += "		vec3 normal_map = normal_map0 * base_v + normal_map1 * map.r + normal_map2 * map.g + normal_map3 * map.b + normal_map4 * map.a;\n";
                    break;
            }
        }
        if (lights != null) {
            if (normalmap != ShaderID.NormalMap.NONE) {
                out += "		N = calcNormalFromMap(normalize(v_N),normal_map);\n";
                out += "		R = reflect(normalize(v_V), calcNormalFromMap(normalize(v_N),normal_map));\n\n";
            } else {
                out += "		N = normalize(v_N);\n";
                out += "		R = reflect(normalize(v_V), normalize(v_N));\n\n";
            }
            out += "		vec4 color = gl_FrontMaterial.ambient * gl_LightModel.ambient + gl_FrontMaterial.emission;\n\n";
        } else out += "		vec4 color = gl_FrontMaterial.ambient + gl_FrontMaterial.emission;\n\n";
        if (lighting != ShaderID.Lighting.NONE) out += lightString + "\n";
        switch(texturing) {
            case SINGLE_TEXTURE:
            case GLOWING_TEXTURE:
                out += "		vec4 texel = texel0;\n";
                break;
            case SPLAT_MAP_1:
                out += "		vec4 texel = texel0 * base_v + texel1 * map.r;\n";
                break;
            case SPLAT_MAP_2:
                out += "		vec4 texel = texel0 * base_v + texel1 * map.r + texel2 * map.g;\n";
                break;
            case SPLAT_MAP_3:
                out += "		vec4 texel = texel0 * base_v + texel1 * map.r + texel2 * map.g + texel3 * map.b;\n";
                break;
            case SPLAT_MAP_4:
                out += "		vec4 texel = texel0 * base_v + texel1 * map.r + texel2 * map.g + texel3 * map.b + texel4 * map.a;\n";
                break;
        }
        if (lighting == ShaderID.Lighting.LIGHTMAP) {
            out += "		color = max(light_map + color,1.0);\n";
            out += "\n";
        }
        if (fog) {
            if (texturing == ShaderID.Texturing.GLOWING_TEXTURE) out += "		gl_FragColor = fog(color) + texel * (vec4(1.0,1.0,1.0,1.0) - color);\n"; else if (texturing == ShaderID.Texturing.SINGLE_TEXTURE_AND_GLOWING_TEXTURE) out += "		gl_FragColor = fog(texel0 * color) + fog(texel1 * (vec4(1.0,1.0,1.0,1.0) - color));\n"; else if (texturing == ShaderID.Texturing.NONE) out += "		gl_FragColor = fog(color);\n"; else out += "		gl_FragColor = fog(texel * color);\n";
        } else {
            if (texturing == ShaderID.Texturing.GLOWING_TEXTURE) out += "		gl_FragColor = color + texel * (vec4(1.0,1.0,1.0,1.0) - color);\n"; else if (texturing == ShaderID.Texturing.SINGLE_TEXTURE_AND_GLOWING_TEXTURE) out += "		gl_FragColor = texel0 * color + texel1 * (vec4(1.0,1.0,1.0,1.0) - color);\n"; else if (texturing == ShaderID.Texturing.NONE) out += "		gl_FragColor = color;\n"; else out += "		gl_FragColor = texel * color;\n";
        }
        out += "	}\n";
        out += "}\n";
        return out;
    }
}
