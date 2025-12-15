package veins;

import com.civbuddy.veins.geo.shapes.AABBShape;
import com.civbuddy.veins.geo.primitives.Face;
import com.civbuddy.veins.geo.util.GridAlignedFaceOptimizer;
import org.joml.Vector3i;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GeoOptimizerTest {
    @Test
    void simpleTest() {
        AABBShape aabbShape = new AABBShape(new Vector3i(0), new Vector3i(3));
        Collection<Face> faces = aabbShape.getFaces();
        List<Face> faces1 = GridAlignedFaceOptimizer.optimize(new ArrayList<>(faces));
        Assertions.assertEquals(faces1.size(), 6);
    }
}
