package org.team2471.bunnybots2022

import edu.wpi.first.networktables.EntryListenerFlags
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.DriverStation
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import org.team2471.frc.lib.coroutines.delay
import org.team2471.frc.lib.coroutines.parallel
import org.team2471.frc.lib.coroutines.periodic
import org.team2471.frc.lib.framework.use
import org.team2471.frc.lib.math.Vector2
import org.team2471.frc.lib.motion.following.driveAlongPath
import org.team2471.frc.lib.motion_profiling.Autonomi
import org.team2471.frc.lib.util.measureTimeFPGA
import org.team2471.frc2022.AprilTag
import java.io.File

private lateinit var autonomi: Autonomi


enum class Side {
    LEFT,
    RIGHT;

    operator fun not(): Side = when (this) {
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}

private var startingSide = Side.RIGHT


object AutoChooser {
    private val isRedAllianceEntry = NetworkTableInstance.getDefault().getTable("FMSInfo").getEntry("isRedAlliance")

    var cacheFile: File? = null
    var redSide: Boolean = true
        get() = isRedAllianceEntry.getBoolean(true)
        set(value) {
            field = value
            isRedAllianceEntry.setBoolean(value)
        }

    private val lyricsChooser = SendableChooser<String?>().apply {
        setDefaultOption("Country roads", "Country roads")
        addOption("take me home", "take me home")
    }

    private val testAutoChooser = SendableChooser<String?>().apply {
        addOption("None", null)
        addOption("20 Foot Test", "20 Foot Test")
        addOption("8 Foot Straight", "8 Foot Straight")
        addOption("April Test Auto", "April Test Auto")

//        addOption("8 Foot Straight Downfield", "8 Foot Straight Downfield")
//        addOption("8 Foot Straight Upfield", "8 Foot Straight Upfield")
//        addOption("8 Foot Straight Sidefield", "8 Foot Straight Sidefield")
        addOption("2 Foot Circle", "2 Foot Circle")
        addOption("4 Foot Circle", "4 Foot Circle")
        addOption("8 Foot Circle", "8 Foot Circle")
        addOption("Hook Path", "Hook Path")
        addOption("Right Complex", "Right Complex")
        setDefaultOption("90 Degree Turn", "90 Degree Turn")


    }

    private val autonomousChooser = SendableChooser<String?>().apply {
        setDefaultOption("Tests", "testAuto")
        addOption("Rotary", "rotaryAuto")
        addOption("Straight Auto", "Straight Auto")
        addOption("Straight Blue Auto", "Straight Blue Auto")
        addOption("aprilTestAuto", "aprilTestAuto")
        addOption("rightAuto", "rightAuto")
        addOption("rightBlueAuto", "rightBlueAuto")
        addOption("2023Test", "2023Test")



    }

    init {
//        DriverStation.reportWarning("Starting auto init warning", false)
//        DriverStation.reportError("Starting auto init error", false)         //            trying to get individual message in event log to get timestamp -- untested

        SmartDashboard.putData("Best Song Lyrics", lyricsChooser)
        SmartDashboard.putData("Tests", testAutoChooser)
        SmartDashboard.putData("Autos", autonomousChooser)

        try {

            cacheFile = File("/home/lvuser/autonomi.json")
            if (cacheFile != null) {
                autonomi = Autonomi.fromJsonString(cacheFile?.readText())!!
                println("Autonomi cache loaded.")
            } else {
                println("Autonomi failed to load!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! RESTART ROBOT!!!!!!")
            }
        } catch (_: Throwable) {
            DriverStation.reportError("Autonomi cache could not be found", false)
            autonomi = Autonomi()
        }
        println("In Auto Init. Before AddListener. Hi.")
        NetworkTableInstance.getDefault()
            .getTable("PathVisualizer")
            .getEntry("Autonomi").addListener({ event ->
                println("Automous change detected")
                val json = event.value.string
                if (json.isNotEmpty()) {
                    val t = measureTimeFPGA {
                        autonomi = Autonomi.fromJsonString(json) ?: Autonomi()
                    }
                    println("Loaded autonomi in $t seconds")
                    if (cacheFile != null) {
                        println("CacheFile != null. Hi.")
                        cacheFile!!.writeText(json)
                    } else {
                        println("cacheFile == null. Hi.")
                    }
                    println("New autonomi written to cache")
                } else {
                    autonomi = Autonomi()
                    DriverStation.reportWarning("Empty autonomi received from network tables", false)
                }
            }, EntryListenerFlags.kImmediate or EntryListenerFlags.kNew or EntryListenerFlags.kUpdate)
    }

    suspend fun autonomous() = use(Drive, name = "Autonomous") {
        println("Got into Auto fun autonomous. Hi. 888888888888888 ${Robot.recentTimeTaken()}")
        val selAuto = SmartDashboard.getString("Autos/selected", "no auto selected")
        SmartDashboard.putString("autoStatus", "init")
        println("Selected Auto = *****************   $selAuto ****************************  ${Robot.recentTimeTaken()}")
        when (selAuto) {
            "Tests" -> testAuto()
            "Carpet Bias Test" -> carpetBiasTest()
            "aprilTestAuto" -> aprilTestAuto()
            "2023Test" -> test2023()
            else -> println("No function found for ---->$selAuto<-----  ${Robot.recentTimeTaken()}")
        }
        SmartDashboard.putString("autoStatus", "complete")
        println("finished autonomous  ${Robot.recentTimeTaken()}")
    }

    private suspend fun testAuto() {
        val testPath = SmartDashboard.getString("Tests/selected", "no test selected") // testAutoChooser.selected
        if (testPath != null) {
            val testAutonomous = autonomi["Tests"]
            val path = testAutonomous?.get(testPath)
            if (path != null) {
                Drive.driveAlongPath(path, true)
            }
        }
    }

    suspend fun carpetBiasTest() = use(Drive) {
        val auto = autonomi["Carpet Bias Test"]
        if (auto != null) {
            var path = auto["01- Forward"]
            Drive.driveAlongPath(path, false)
            path = auto["02- Backward"]
            Drive.driveAlongPath(path, false)
            path = auto["03- Left"]
            Drive.driveAlongPath(path, false)
            path = auto["04- Forward"]
            Drive.driveAlongPath(path, false)
            //path = auto["05- Backward"]
        }
    }


    suspend fun test8FtStraight() = use(Drive) {
        val auto = autonomi["Tests"]
        if (auto != null) {
            val path = auto["8 Foot Straight"]
            Drive.driveAlongPath(path, true)
        }
    }

    suspend fun test8FtCircle() = use(Drive) {
        val auto = autonomi["Tests"]
        if (auto != null) {
            val path = auto["8 Foot Circle"]
            Drive.driveAlongPath(path, true)
        }
    }


    suspend fun test90DegreeTurn() = use(Drive) {
        val auto = autonomi["Tests"]
        if (auto != null) {
            Drive.driveAlongPath(auto["90 Degree Turn"], true, 2.0)
        }
    }
    suspend fun bunnyAutoOne() = use(Drive){
        val auto = autonomi["Tests"]
        if (auto != null){
            Drive.driveAlongPath(auto[""], true, 2.0)
        }
    }

    suspend fun test2023() = use(Drive) {
        val auto = autonomi["2023Test"]
        if (auto != null) {
            var path = auto["StraightOut"]
            Drive.driveAlongPath(path, true, 0.0, true)
            path = auto["StraightBack"]
            Drive.driveAlongPath(path, false)
            path = auto["StraightOut"]
            Drive.driveAlongPath(path, true)
            path = auto["StraightBack"]
            Drive.driveAlongPath(path, false)
        }
    }
    suspend fun aprilTestAuto() = use(Drive) {
        val auto = autonomi["Bunny Bot Simple"]
        if(auto != null) {
            var path = auto["1 - Forward"]
            var pathDone = false
            parallel({
                Drive.driveAlongPath(path, true, 0.0, true)
                println("Done Driving")
                pathDone = true
            }, {
                periodic {
//                    println("Gone to periodic + ${AprilTag.validTarget}")
                    AprilTag.resetLastResult()
                    if (AprilTag.validTarget) {
                        Drive.position = Vector2(Drive.position.x + AprilTag.xOffset, Drive.position.y)
                        println("Modified X offset: ${AprilTag.xOffset} Drive.position.x: ${Drive.position.x}")
                    }


                    if (pathDone) {
                        this.stop()
                    }
                }
            })
        }
    }
}
