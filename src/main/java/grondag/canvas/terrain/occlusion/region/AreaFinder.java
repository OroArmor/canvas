package grondag.canvas.terrain.occlusion.region;

import java.util.Arrays;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AreaFinder {
	private static final Area[] AREA;

	public static final int AREA_COUNT;

	private static final Area[] SECTION;

	public static final int SECTION_COUNT;

	public Area get(int index) {
		return AREA[index];
	}

	public Area getSection(int sectionIndex) {
		return SECTION[sectionIndex];
	}

	static {
		final IntOpenHashSet areas = new IntOpenHashSet();

		areas.add(AreaUtil.areaKey(0, 0, 15, 15));

		areas.add(AreaUtil.areaKey(1, 0, 15, 15));
		areas.add(AreaUtil.areaKey(0, 0, 14, 15));
		areas.add(AreaUtil.areaKey(0, 1, 15, 15));
		areas.add(AreaUtil.areaKey(0, 0, 15, 14));

		for (int x0 = 0; x0 <= 15; x0++) {
			for (int x1 = x0; x1 <= 15; x1++) {
				for (int y0 = 0; y0 <= 15; y0++) {
					for(int y1 = y0; y1 <= 15; y1++) {
						areas.add(AreaUtil.areaKey(x0, y0, x1, y1));
					}
				}
			}
		}

		AREA_COUNT = areas.size();

		AREA = new Area[AREA_COUNT];

		int i = 0;

		for(final int k : areas) {
			AREA[i++] = new Area(k, 0);
		}

		Arrays.sort(AREA, (a, b) -> {
			final int result = Integer.compare(b.areaSize, a.areaSize);

			// within same area size, prefer more compact rectangles
			return result == 0 ? Integer.compare(a.edgeCount, b.edgeCount) : result;
		});

		// PERF: minor, but sort keys instead array to avoid extra alloc at startup
		for (int j = 0; j < AREA_COUNT; j++) {
			AREA[j] = new Area(AREA[j].areaKey, j);
		}

		final ObjectArrayList<Area> sections = new ObjectArrayList<>();

		for (final Area a : AREA) {
			if ((a.x0 == 0  &&  a.x1 == 15) || (a.y0 == 0  &&  a.y1 == 15)) {
				sections.add(a);
			}
		}

		SECTION_COUNT = sections.size();
		SECTION = sections.toArray(new Area[SECTION_COUNT]);

	}

	final long[] bits = new long[4];

	public final ObjectArrayList<Area> areas =  new ObjectArrayList<>();

	public void find(long[] bitsIn, int sourceIndex) {
		areas.clear();
		final long[] bits = this.bits;
		System.arraycopy(bitsIn, sourceIndex, bits, 0, 4);

		final long hash = AreaUtil.areaHash(bits);

		for(final Area r : AREA) {
			if (r.matchesHash(hash) && r.isIncludedBySample(bits, 0)) {
				areas.add(r);
				r.clearBits(bits, 0);

				if (hash == 0) {
					break;
				}
			}
		}
	}

	public void find(long[] bitsIn, int sourceIndex, Consumer<Area> consumer) {
		areas.clear();
		final long[] bits = this.bits;
		System.arraycopy(bitsIn, sourceIndex, bits, 0, 4);

		int bitCount = Long.bitCount(bits[0]) +  Long.bitCount(bits[1]) +  Long.bitCount(bits[2]) +  Long.bitCount(bits[3]);

		if (bitCount == 0) {
			return;
		}

		final long hash = AreaUtil.areaHash(bits);

		for(final Area r : AREA) {
			if (r.matchesHash(hash) && r.isIncludedBySample(bits, 0)) {
				consumer.accept(r);
				r.clearBits(bits, 0);
				bitCount -= r.areaSize;

				if (bitCount == 0) {
					break;
				}
			}
		}
	}

	public void findSections(long[] bitsIn, int sourceIndex, Consumer<Area> consumer) {
		areas.clear();
		final long[] bits = this.bits;
		System.arraycopy(bitsIn, sourceIndex, bits, 0, 4);

		final int bitCount = Long.bitCount(bits[0]) + Long.bitCount(bits[1]) + Long.bitCount(bits[2]) + Long.bitCount(bits[3]);

		if (bitCount == 0) {
			return;
		}

		final long hash = AreaUtil.areaHash(bits);

		for(final Area r : SECTION) {
			if (r.matchesHash(hash) && r.isIncludedBySample(bits, 0)) {
				consumer.accept(r);
			}
		}
	}

	// PERF: start later in search for smaller samples
	// or search by hash
	public Area findLargest(long[] bitsIn, int sourceIndex) {
		for(final Area r : AREA) {
			if (r.isIncludedBySample(bitsIn, sourceIndex)) {
				return r;
			}
		}

		return  null;
	}
}