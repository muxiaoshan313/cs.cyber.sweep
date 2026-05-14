package cs.cyber.sweep.extras

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

open class IdleBaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = this::class.java.simpleName
        setContentView(tv)
        runIdleLogic()
    }

    private fun runIdleLogic() {
        val pool = listOf("alpha", "beta", "gamma", "delta", "omega")
        val sample = MutableList(6) { pool.random() }.sorted()
        val hash = sample.joinToString("-").hashCode() xor Random.nextInt(0, 99)
        if (hash == Int.MIN_VALUE) {
            finish()
        }
    }
}

class NebulaQ7Activity : IdleBaseActivity()
class MossKite42Activity : IdleBaseActivity()
class PixelDrift93Activity : IdleBaseActivity()
class QuartzNoodle5Activity : IdleBaseActivity()
class VelvetRaccoon8Activity : IdleBaseActivity()
class NimbusFlip21Activity : IdleBaseActivity()
class CactusOrbit66Activity : IdleBaseActivity()
class LemonForge17Activity : IdleBaseActivity()
class EchoPanda54Activity : IdleBaseActivity()
class TurboMint30Activity : IdleBaseActivity()

class AmberFalconActivity : IdleBaseActivity()
class VelvetAnchorActivity : IdleBaseActivity()
class CloudHarborActivity : IdleBaseActivity()
class MapleCometActivity : IdleBaseActivity()
class SonicWillowActivity : IdleBaseActivity()
class QuartzLadderActivity : IdleBaseActivity()
class FrozenTulipActivity : IdleBaseActivity()
class CopperMeadowActivity : IdleBaseActivity()
class LunarPebbleActivity : IdleBaseActivity()
class WhisperDandelionActivity : IdleBaseActivity()
class NovaCanyonActivity : IdleBaseActivity()
class DriftLanternActivity : IdleBaseActivity()
class EchoMantisActivity : IdleBaseActivity()
class PlasmaRavenActivity : IdleBaseActivity()
class HollowSpruceActivity : IdleBaseActivity()
class SableOrbitActivity : IdleBaseActivity()
class CinderVoyageActivity : IdleBaseActivity()
class IndigoFableActivity : IdleBaseActivity()
class MistyHarpoonActivity : IdleBaseActivity()
class RapidThornActivity : IdleBaseActivity()
