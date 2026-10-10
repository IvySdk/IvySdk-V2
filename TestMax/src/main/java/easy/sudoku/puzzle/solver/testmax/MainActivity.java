package easy.sudoku.puzzle.solver.testmax;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkInitializationConfiguration;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //easy.sudoku.puzzle.solver.free
        //I41gHx5x77_0BATnMJq03L3-9pI1QWmkbnOuiUprcorozOTJiNKniNmtB2vcZfL9Rl28DrzrdpcbCAwvTEfGcQ

        //com.oakever.tiletrip
        //iny0rUIa1b4uEq5K2eOLKffFG5AiIeJR3HTWuLV9T1oEQQoWQb89XB1wyqmNVcrevnHSwXTqTBs0_P3ae9Iygx


//        MaxSegmentCollection.Builder segmentCollectionBuilder = MaxSegmentCollection.builder();
//        List<Integer> list = new ArrayList<>();
//        list.add(1);
//        segmentCollectionBuilder.addSegment(new MaxSegment(3003, list));
//        segmentCollectionBuilder.addSegment(new MaxSegment(2005, list));
//        segmentCollectionBuilder.addSegment(new MaxSegment(2004, list));
//        segmentCollectionBuilder.addSegment(new MaxSegment(1003, list));
//        segmentCollectionBuilder.addSegment(new MaxSegment(3005, list));
//        segmentCollectionBuilder.addSegment(new MaxSegment(2003, list));
//        segmentCollectionBuilder.addSegment(new MaxSegment(3004, list));

//        List<String> adunits = new ArrayList<>();
//        adunits.add("3d334ae5c61813be");
//        adunits.add("b9ce3e4b9d85111a");
//        adunits.add("acfe40aab6d5610a");
//        adunits.add("b2206899fd77da58");
//        adunits.add("3386ee4e5cd1c25f");

        AppLovinSdkInitializationConfiguration initConfig = AppLovinSdkInitializationConfiguration
                .builder("iny0rUIa1b4uEq5K2eOLKffFG5AiIeJR3HTWuLV9T1oEQQoWQb89XB1wyqmNVcrevnHSwXTqTBs0_P3ae9Iygx")
                .setMediationProvider(AppLovinMediationProvider.MAX)
//                .setAdUnitIds(adunits)
                .build();
        AppLovinSdk.getInstance(this).initialize(initConfig, new AppLovinSdk.SdkInitializationListener() {
            @Override
            public void onSdkInitialized(AppLovinSdkConfiguration appLovinSdkConfiguration) {
                AppLovinSdk.getInstance(MainActivity.this).showMediationDebugger();
                AppLovinSdk.getInstance(MainActivity.this).getSettings().setVerboseLogging(true);
            }
        });
    }
}