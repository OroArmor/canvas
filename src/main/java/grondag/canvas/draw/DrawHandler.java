package grondag.canvas.draw;

import net.fabricmc.fabric.impl.client.indigo.renderer.RenderMaterialImpl.Value;

import grondag.canvas.pipeline.BufferFormat;
import grondag.canvas.pipeline.PipelineContext;

public interface DrawHandler {
	static DrawHandler get() {
		return null;
	}

	int index();

	BufferFormat inputFormat();

	static DrawHandler get(PipelineContext context, BufferFormat format, Value mat) {
		// TODO Auto-generated method stub
		return null;
	}
}