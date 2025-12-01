package dev.ynki.util.render.providers;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public final class ResourceProvider {
	public static final ShaderProgramKey TEXTURE_SHADER_KEY = new ShaderProgramKey(getShaderIdentifier("texture"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
	public static final ShaderProgramKey RECTANGLE_SHADER_KEY = new ShaderProgramKey(getShaderIdentifier("rectangle"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
	public static final ShaderProgramKey BLUR_SHADER_KEY = new ShaderProgramKey(getShaderIdentifier("blur"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
	public static final ShaderProgramKey RECTANGLE_BORDER_SHADER_KEY = new ShaderProgramKey(getShaderIdentifier("border"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
	public static final ShaderProgramKey GLASS_SHADER_KEY = new ShaderProgramKey(getGlass("data"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
	public static final ShaderProgramKey LIQUID_GLASS_SHADER_KEY = new ShaderProgramKey(getShaderIdentifier("liquid"), VertexFormats.POSITION_COLOR, Defines.EMPTY);

	public static final Identifier firefly = Identifier.of("velyasik", "images/particles/firefly.png");
	public static final Identifier bloom = Identifier.of("velyasik", "images/particles/bloom.png");
	public static final Identifier snowflake = Identifier.of("velyasik", "images/particles/snowflake.png");
	public static final Identifier dollar = Identifier.of("velyasik", "images/particles/dollar.png");
	public static final Identifier heart = Identifier.of("velyasik", "images/particles/heart.png");
	public static final Identifier star = Identifier.of("velyasik", "images/particles/star.png");
	public static final Identifier spark = Identifier.of("velyasik", "images/particles/spark.png");
	public static final Identifier crown = Identifier.of("velyasik", "images/particles/crown.png");
	public static final Identifier lightning = Identifier.of("velyasik", "images/particles/lightning.png");
	public static final Identifier line = Identifier.of("velyasik", "images/particles/line.png");
	public static final Identifier point = Identifier.of("velyasik", "images/particles/point.png");
	public static final Identifier rhombus = Identifier.of("velyasik", "images/particles/rhombus.png");


	public static final Identifier marker = Identifier.of("velyasik", "images/targetesp/target.png");
	public static final Identifier marker2 = Identifier.of("velyasik", "images/targetesp/target2.png");


	public static final Identifier CUSTOM_CAPE = Identifier.of("velyasik", "cape/cape.png");
	public static final Identifier CUSTOM_ELYTRA = Identifier.of("velyasik", "cape/elytra.png");

	public static final Identifier container = Identifier.of("velyasik", "images/hud/container.png");

	public static final Identifier color_image = Identifier.of("velyasik", "images/gui/pick.png");


	private static Identifier getGlass(String name) {
		return Identifier.of("velyasik", "core/glass/" + name);
	}
	private static Identifier getShaderIdentifier(String name) {
		return Identifier.of("velyasik", "core/" + name);
	}
}