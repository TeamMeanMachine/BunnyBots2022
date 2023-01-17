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
            "Straight Auto" -> straightAuto()
            "Straight Blue Auto" -> straightBlueAuto()
            "rightAuto" -> rightAuto()
            "rightBlueAuto" -> rightBlueAuto()
            "aprilTestAuto" -> aprilTestAuto()
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
    suspend fun RightComplex() = use(Drive, Armavator, DepthCharge) {
        val auto = autonomi["Bunny Bot Right"]
        if(auto != null) {
            Armavator.goToDrivePose()
          //  var path = auto["1 - Grab Bunny"]
          //  Drive.driveAlongPath(path, false)
            //pick up bunny
            var path = auto["1 - Forward"]
            Drive.driveAlongPath(path, false)
            //april tag line up
            path = auto["2 - Bumper to Bin"]
            Drive.driveAlongPath(path, false)
            parallel({
                Armavator.goToOverBinPose()
                Armavator.suckMotor.setPercentOutput(-1.0)
                delay(1.0)
                Armavator.suckMotor.setPercentOutput(0.0)
                //eject tube
            }, {
                DepthCharge.score(false)
            })
            Armavator.goToDrivePose()
            path = auto["3 - Bin Backup"]
            Drive.driveAlongPath(path, false)
            DepthCharge.score(true)
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

    suspend fun straightAuto() = use (Drive, Armavator, DepthCharge) {
        val auto = autonomi["Bunny Bot Simple"]
        if(auto != null) {
            var pathDone = false
            var path = auto["1 - Forward"]
            parallel ({
                Drive.driveAlongPath(path, true, 0.0, true)
                pathDone = true
            }, {
                Armavator.startToGroundPose()
                delay(0.3)
                Armavator.goToPose(Pose.OVER_BIN_POSE3)
                Armavator.suckMotor.setPercentOutput(0.0)
            }, {
                Armavator.suckMotor.setPercentOutput(1.0)
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

//            path = auto["2 - Forward Again"]
//            Drive.driveAlongPath(path, resetOdometry = false)
            parallel({
//                Armavator.intakePivotMotor.set(0.0) no longer works
                Armavator.goToPose(Pose.OVER_BIN_POSE4)
                Armavator.suckMotor.setPercentOutput(-1.0)
                Armavator.spitMotor.setPercentOutput(-1.0)
                delay(0.25)
                Armavator.goToPose(Pose.OVER_BIN_POSE3)
            }, {
                DepthCharge.score(false)
            })
            parallel({
                Armavator.goToDrivePose()
            }, {
                path = auto["2 - Bin Backup"]
                Drive.driveAlongPath(path, false)
            }, {
                Armavator.suckMotor.setPercentOutput(0.0)
                Armavator.spitMotor.setPercentOutput(0.0)
                delay(path.duration * 0.5)
                DepthCharge.score(true)
            })
        }
    }

    suspend fun straightBlueAuto() = use (Drive, Armavator, DepthCharge) {
        val auto = autonomi["Bunny Bot Blue Simple"]
        if(auto != null) {
            var pathDone = false
            var path = auto["1 - Forward"]
            parallel ({
                Drive.driveAlongPath(path, true, 0.0, true)
                pathDone = true
            }, {
                Armavator.startToGroundPose()
                delay(0.3)
                Armavator.goToPose(Pose.OVER_BIN_POSE3)
                Armavator.suckMotor.setPercentOutput(0.0)
            }, {
                Armavator.suckMotor.setPercentOutput(1.0)
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

//            path = auto["2 - Forward Again"]
//            Drive.driveAlongPath(path, resetOdometry = false)
            parallel({
//                Armavator.intakePivotMotor.set(0.0) no longer works
                Armavator.goToPose(Pose.OVER_BIN_POSE4)
                Armavator.suckMotor.setPercentOutput(-1.0)
                Armavator.spitMotor.setPercentOutput(-1.0)
                delay(0.25)
                Armavator.goToPose(Pose.OVER_BIN_POSE3)
            }, {
                DepthCharge.score(false)
            })
            parallel({
                Armavator.goToDrivePose()
            }, {
                path = auto["2 - Bin Backup"]
                Drive.driveAlongPath(path, false)
            }, {
                Armavator.suckMotor.setPercentOutput(0.0)
                Armavator.spitMotor.setPercentOutput(0.0)
                delay(path.duration * 0.5)
                DepthCharge.score(true)
            })
        }
    }

    suspend fun rightAuto() = use (Drive, Armavator, DepthCharge) {
        val auto = autonomi["Right Auto"]
        if(auto != null) {
            var path = auto["1 - Straight To Bin"]
            parallel ({
                Drive.driveAlongPath(path, true, 0.0, true)
            }, {
                Armavator.startToGroundPose()
                delay(0.3)
                Armavator.goToPose(Pose.OVER_BIN_POSE3)
                Armavator.suckMotor.setPercentOutput(0.0)
            }, {
                Armavator.suckMotor.setPercentOutput(1.0)
            })
            parallel({
                Armavator.goToPose(Pose.OVER_BIN_POSE4)
                Armavator.suckMotor.setPercentOutput(-1.0)
                Armavator.spitMotor.setPercentOutput(-1.0)
                delay(0.25)
                Armavator.goToPose(Pose.OVER_BIN_POSE3)
            }, {
                DepthCharge.score(true)
            })
            parallel({
                Armavator.goToDrivePose()
            }, {
                path = auto["2 - Get Close to Bin"]
                Drive.driveAlongPath(path, false)
            }, {
                Armavator.suckMotor.setPercentOutput(0.0)
                Armavator.spitMotor.setPercentOutput(0.0)
                delay(path.duration * 0.5)
                DepthCharge.score(false)
            })
        }
    }

    suspend fun rightBlueAuto() = use (Drive, Armavator, DepthCharge) {
        val auto = autonomi["Right Blue Auto"]
        if(auto != null) {
            var path = auto["1 - Straight To Bin"]
            parallel ({
                Drive.driveAlongPath(path, true, 0.0, true)
            }, {
                Armavator.startToGroundPose()
                delay(0.3)
                Armavator.goToPose(Pose.OVER_BIN_POSE3)
                Armavator.suckMotor.setPercentOutput(0.0)
            }, {
                Armavator.suckMotor.setPercentOutput(1.0)
            })
            parallel({
                Armavator.goToPose(Pose.OVER_BIN_POSE4)
                Armavator.suckMotor.setPercentOutput(-1.0)
                Armavator.spitMotor.setPercentOutput(-1.0)
                delay(0.25)
                Armavator.goToPose(Pose.OVER_BIN_POSE3)
            }, {
                DepthCharge.score(true)
            })
            parallel({
                Armavator.goToDrivePose()
            }, {
                path = auto["2 - Get Close to Bin"]
                Drive.driveAlongPath(path, false)
            }, {
                Armavator.suckMotor.setPercentOutput(0.0)
                Armavator.spitMotor.setPercentOutput(0.0)
                delay(path.duration * 0.5)
                DepthCharge.score(false)
            })
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
