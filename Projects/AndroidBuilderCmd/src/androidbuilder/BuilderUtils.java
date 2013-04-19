package androidbuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import java.io.IOException;
import java.util.Properties;

/***
 * This is a modified version of the BuilderUtils taken from (decompiled)
 * AndroidBuilder.jar from
 * http://forum.unity3d.com/threads/129042-Android-Builder-Tool/page4 so we can
 * run it as a commandline tool.
 */
public class BuilderUtils {
	public static final int WINDOWS_OS_TYPE = 0;
	public static final int MAC_OS_TYPE = 1;
	public static final int INSTALLER_APK = 2;
	public static final int GAMEDATA_APK = 3;
	public static final String OUT_INSTALLER_UNAL_APK = "installer_unaligned.apk";
	public static final String NAME_GAMEDATA = "_gameData";
	public static final String NAME_INSTALLER = "_Installer";
	private static String inputApkPath;
	private static String inputApkNameNoExt;
	public static String apkInstallerPath;
	public static String apkGameDataPath;
	public static String pathStagingArea = "";
	private static Process activeProc = null;
	private static BufferedReader stdInput = null;
	private static BufferedReader stdError = null;

	public static String toolsPath = "";
	public static String androidJarPath = "";
	public static String androidPlatformToolsPath = "";
	public static String androidSdkToolsPath = "";
	public static String installerProjectPath = "";
	public static String installerResPath = "";
	public static String installerClassesPath = "";
	public static String androidSdkRoot = "";
	public static String unityProjectPath = "";
	public static boolean debugMode = false;
	public static boolean debugCert = false;
	public static String keystoreFile = "";//signDialog.txtKeystoreFile.getText().trim();
	public static String keystorePass = "";//signDialog.txtKeystorePass.getText().trim();
	public static String aliasName = "";//signDialog.listAlias.getSelectedItem().toString();
	public static String aliasPass = "";//signDialog.txtAliasPass.getText().trim();

	public static void main(String[] args) {
		System.out.println("args:");
		for (int i = 0; i < args.length; ++i) {
			System.out.println(args[i]);
		}

		// Process cmd arguments
		for (int i = 0; i < args.length; ++i) {
			String arg = args[i];

			if (arg.equalsIgnoreCase("-t")) {
				// Tools path
				if (++i >= args.length) {
					printUsage();
					System.out.println("Missing Tools Path");
					return;
				}
				toolsPath = args[i];
			} else if (arg.toLowerCase().equals("-r")) {
				// Tools path
				if (++i >= args.length) {
					printUsage();
					System.out.println("Missing Res Path");
					return;
				}
				installerProjectPath = args[i];
			} else if (arg.toLowerCase().equals("-u")) {
				// Tools path
				if (++i >= args.length) {
					printUsage();
					System.out.println("Missing Unity Project Path");
					return;
				}
				unityProjectPath = args[i];
			}	else if (arg.toLowerCase().equals("-a")) {
				// Android sdk root
				if (++i >= args.length) {
					printUsage();
					System.out.println("Missing Android SDK Root");
					return;
				}
				androidSdkRoot = args[i];
			} else if (arg.toLowerCase().equals("-s")) {
				// Signing
				if (i+4 >= args.length) {
					printUsage();
					System.out.println("Missing Keystore Settings");
					return;
				}
				keystoreFile = args[++i];
				keystorePass = args[++i];
				aliasName = args[++i];
				aliasPass = args[++i];
			} else if (arg.toLowerCase().equals("-dm")) {
				// Debug mode
				if (++i >= args.length) {
					debugMode = true;
				}
				else
				{
					debugMode = args[i].toLowerCase().equals("on");
				}
			} else if (arg.toLowerCase().equals("-dc")) {
				// Debug cert
				if (++i >= args.length) {
					debugCert = true;
				}
				else
				{
					debugCert = args[i].toLowerCase().equals("on");
				}
			} else {
				// Default to setting the input Apk path
				inputApkPath = FilenameUtils.getFullPath(arg);
				System.out.println("using inputApkPath=" + inputApkPath);
				inputApkNameNoExt = FilenameUtils.getBaseName(arg);
				System.out.println("using inputApkNameNoExt="
						+ inputApkNameNoExt);
			}
		}

		// Validate settings
		if (inputApkPath.equals("") || toolsPath.equals("")
				|| installerProjectPath.equals("") || unityProjectPath.equals("") ||
				(!debugMode && !debugCert && (keystoreFile.equals("") || keystorePass.equals("") || aliasName.equals("") || aliasPass.equals("")))) {
			printUsage();
			return;
		}

		// Tweak settings
		if (!toolsPath.trim().endsWith(File.separator)) {
			toolsPath = toolsPath.trim() + File.separator;
		}
		if (!unityProjectPath.trim().endsWith(File.separator)) {
			unityProjectPath = unityProjectPath.trim() + File.separator;
		}
		if (!installerProjectPath.trim().endsWith(File.separator)) {
			installerProjectPath = installerProjectPath.trim() + File.separator;
		}
		
		// Prepare subpaths
		installerResPath = installerProjectPath + "res" + File.separator;
		installerClassesPath = installerProjectPath + "bin" + File.separator + "classes" + File.separator; 

		// Set up android tools
		if (androidSdkRoot.equals(""))
		{
			System.err.println("\nAuto locate Unity Android SDK..");
			androidSdkRoot = getUnityAndroidSDKRootSetting();

		}
		if (androidSdkRoot.trim().length() == 0) {
			System.err
					.println("\nUnity Android SDK Root Path hasn't been set! The build process can't continue!!!\nQuit, setup Unity Android Sdk root path, make sure you have the latest Android API installed and try building again.");

			return;
		}

		androidSdkToolsPath = androidSdkRoot + File.separator + "tools"
				+ File.separator;
		androidPlatformToolsPath = androidSdkRoot + File.separator
				+ "platform-tools" + File.separator;

		int maxApiLevelFound = 0;
		File searchDir = new File(androidSdkRoot + File.separator + "platforms");
		String[] files = searchDir.list(DirectoryFileFilter.INSTANCE);
		for (int i = 0; i < files.length; i++) {
			if (files[i].toLowerCase().contains("android-")) {
				String strApiLevel = files[i].substring(files[i]
						.lastIndexOf("-") + 1);
				int apiLevel = Integer.parseInt(strApiLevel);
				if (maxApiLevelFound < apiLevel) {
					maxApiLevelFound = apiLevel;
				}
			}

		}

		androidJarPath = androidSdkRoot + File.separator + "platforms"
				+ File.separator + "android-" + maxApiLevelFound
				+ File.separator + "android.jar";

		System.out.println("\nLatest Android API found: " + androidJarPath);

		// Finally, do stuff!
		doBuild();
	}

	public static void printUsage() {
		System.err.println("Arguments: (note you should use absolute paths)\n"
						+ "-t Path to tools (folder containing this jar)\n"
						+ "-r Path to res folder with resources for installer\n"
						+ "-u Path to Unity project\n"
						+ "-a Path to android tools root\n"
						+ "-dm Turns debug mode on or off\n"
						+ "-dc Turns debug certificate on or off\n"
						+ "-s Specifies non-debug signing details (as specified in Unity Android Signing settings)"
						+ "<path/to/package.apk> -t <path/to/tools> -r <path/to/res> -u <path/to/unity/project> [-a <path/to/android>] [-dm [<on|off>]] [-dc [on|off]] [-s <path/to/keystorefile> <keystorePassword> <alias> <aliasPassword>]");
	}

	public static void doBuild() {
		System.out
				.println("\nCreating initial GameData APK from Unity generated APK...");
		if (!createInitialApkFromUnityApk()) {
			System.err
					.println("\nError! Android Builder failed at createInitialApkFromUnityApk(...)!");
			return;
		}

		System.out.println("\nCleaning initial APK...");
		System.out.println("\n-------------------------------------------");
		if (!cleanApk(3)) {
			System.err
					.println("\nError! Android Builder failed at cleanApk(3)!");
			return;
		}
		System.out.println("\n-------------------------------------------");
		System.out.println("\nFinished cleaning APK...");

		System.out.println("\n\nRebuilding Installer APK...");
		System.out.println("\n-------------------------------------------");
		System.out.println("\nStep 1: Preparing Unity StagingArea folders...");
		if (!prepareUnityStagingAreaFolders()) {
			System.err
					.println("\nError! Android Builder failed in Step 1: prepareUnityStagingAreaFolders()");
			return;
		}

		System.out
				.println("\n\nStep 2: Copying Installer Android resources folder to the StagingArea...");
		if (!copyInstallerResFolders()) {
			System.err
					.println("\nError! Android Builder failed in Step 2: copyInstallerResFolders(...)");
			return;
		}

		System.out.println("\n\nStep 3: Generating and compiling R.java class...");
		if (!generateAndCompileRClass()) {
			System.err
					.println("\nError! Android Builder failed in Step 3: generateAndCompileRClass()");
			return;
		}

		System.out.println("\n\nStep 4: Copying Installer classes to the StagingArea...");
		if (!copyInstallerClasses()) {
			System.err
					.println("\nError! Android Builder failed in Step 4: copyInstallerClasses()");
			return;
		}

		System.out.println("\n\nStep 5: Generating DEX file...");
		if (!generateDexFile()) {
			System.err
					.println("\nError! Android Builder failed in Step 5: generateDexFile()");
			return;
		}

		System.out
				.println("\n\nStep 6: Preparing Unity native libs for packaging...");
		if (!prepareUnityNativeLibsForApk()) {
			System.err
					.println("\nError! Android Builder failed in Step 6: prepareUnityNativeLibsForApk()");
			return;
		}

		System.out.println("\n\nStep 7: Creating Installer unaligned APK...");
		if (!createUnalignedInstallerApk(debugMode)) {
			System.err
					.println("\nError! Android Builder failed in Step 7: createUnalignedInstallerApk()");
			return;
		}

		if (debugMode) {
			System.out
					.println("\n\nStep 8: Signing Installer unaligned APK with debug certificate...");
			if (!signApkWithDebugKey(pathStagingArea
					+ "installer_unaligned.apk")) {
				System.err
						.println("\nError! Android Builder failed in Step 8: signApkWithDebugKey(...)");
				return;
			}
		}

		System.out
				.println("\n\nStep 9: Outputting Installer unaligned APK to user selected path:\n"
						+ apkInstallerPath);

		if (!outputUnalignedInstallerApk()) {
			System.err
					.println("\nError! Android Builder failed in Step 9: outputUnalignedInstallerApk()");
			return;
		}

		System.out
				.println("\n\nStep 10: Applying zipalign for Installer APK...");
		if (!checkZipAlignStatusForApk(2, false, true)) {
			System.err
					.println("\nError! Android Builder failed in Step 10: checkZipAlignStatusForApk(...)");
			return;
		}
		System.out.println("\n-------------------------------------------");
		System.out.println("\nFinished generating Installer APK");
		System.out.println("\n-------------------------------------------");

		System.out.println("\n\nApplying zipalign for GameData APK...");
		System.out.println("\n-------------------------------------------");
		checkZipAlignStatusForApk(3, true, true);
		System.out.println("\n-------------------------------------------");

		System.out.println("\nFinished generating the APK files...");
		System.out.println("\n\n===========================================");

		if (!debugMode) {
			if (debugCert) {
				System.out
						.println("\n\nSigning Installer APK with debug certificate...");
				if (!signApkWithDebugKey(apkInstallerPath)) {
					System.err
							.println("\nError! Android Builder failed in post debug sign step!");
					return;
				}

				System.out
						.println("\n\nApplying zipalign for Installer APK...");
				if (!checkZipAlignStatusForApk(2, false, true)) {
					System.err
							.println("\nError! Android Builder failed while zip aligning the user signed APK while post debug signing "
									+ apkInstallerPath);

					return;
				}
			} else {

				System.out.println("\n\nSigning Installer APK with user certificate...");
				if (!signApkWithUserKey(apkInstallerPath, keystoreFile,	keystorePass, aliasName, aliasPass)) 
				{
					System.err.println("\nError while trying to sign APK with user certificate:");
					System.err.println("\nAPK File: "	+ apkInstallerPath);
					System.err.println("\nKeystore file: " + keystoreFile);
					System.err.println("\nKeystore pass: " + keystorePass);
					System.err.println("\nAlias name: " + aliasName);
					System.err.println("\nAlias pass: " + aliasPass);
					return;
				}

				System.out.println("\n\nApplying zipalign for Installer APK...");
				if (!checkZipAlignStatusForApk(2, false, true)) {
					System.err.println("\nError! Android Builder failed while zip aligning the user signed APK:"
									+ apkInstallerPath);

					return;
				}
			}
		}

    System.out.println("\n\nYou can close this abomination!");
    System.out.println("\nFinished creating:\n");
    System.out.println("\n" + apkInstallerPath);
    System.out.println("\n" + apkGameDataPath);
    System.out.println("\n===========================================");

		return;
	}

	public static void getActiveProcessConsoleOutput(boolean waitForProcess) {
		try {
			String output;
			while ((output = stdInput.readLine()) != null) {
				System.out.println("\n" + output);
			}
			if (waitForProcess)
				activeProc.waitFor();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void getActiveProcessErrorOutput(boolean waitForProcess) {
		try {
			String output;
			while ((output = stdError.readLine()) != null) {
				System.err.println("\n" + output);
			}

			if (waitForProcess)
				activeProc.waitFor();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static String getStrApkType(int apkType) {
		if (apkType == 2)
			return "Installer";
		if (apkType == 3) {
			return "GameData";
		}

		return "";
	}

	public static String getPathApkType(int apkType) {
		if (apkType == 2)
			return apkInstallerPath;
		if (apkType == 3) {
			return apkGameDataPath;
		}

		return "";
	}

	public static String execCmd(boolean waitToFinish, String[] arguments) {
		StringBuilder output = new StringBuilder();

		ArrayList command = new ArrayList();
		for (String arg : arguments) {
			command.add(arg);
		}

		String s = "";
		try {
			ProcessBuilder procBuilder = new ProcessBuilder(command);
			activeProc = procBuilder.start();

			stdInput = new BufferedReader(new InputStreamReader(
					activeProc.getInputStream()));

			stdError = new BufferedReader(new InputStreamReader(
					activeProc.getErrorStream()));

			if (waitToFinish) {
				while ((s = stdInput.readLine()) != null) {
					output.append('\n');
					output.append(s);
				}

				while ((s = stdError.readLine()) != null) {
					output.append('\n');
					output.append(s);
				}

				activeProc.waitFor();
			}

			return output.toString();
		} catch (Exception ex) {
			System.err
					.print("\nError in execCmd(...) trying to execute command: "
							+ (String) command.get(0) + "\n" + ex.getMessage());
			ex.printStackTrace();
		}

		return "";
	}

	public static boolean createInitialApkFromUnityApk() {
		try {
			apkInstallerPath = inputApkPath + inputApkNameNoExt + "_Installer"
					+ ".apk";
			apkGameDataPath = inputApkPath + inputApkNameNoExt + "_gameData"
					+ ".apk";

			FileUtils.copyFile(new File(inputApkPath + inputApkNameNoExt
					+ ".apk"), new File(apkGameDataPath));
			System.out.print("\nCreated " + getStrApkType(3) + " APK...");
		} catch (Exception ex) {
			ex.printStackTrace();
			System.err.print("\nERROR while creating initial APK file "
					+ inputApkNameNoExt + ":\n" + ex.getMessage());

			return false;
		}

		return true;
	}

	public static boolean cleanApk(int apkType) {
		if (apkType == 2) {
			String output = execCmd(false, new String[] { "java", "-jar",
					toolsPath + "aaptExtTool.jar", androidPlatformToolsPath,
					"-removefolder", apkInstallerPath, "assets", "splash.png",
					"settings.xml" });

			getActiveProcessConsoleOutput(true);

			if (activeProc.exitValue() != 0) {
				System.err
						.print("\nError while trying to clean INSTALLER APK!");
				return false;
			}
		} else if (apkType == 3) {
			System.out.print("\nRemoving classes.dex file...");
			String output = execCmd(true, new String[] {
					androidPlatformToolsPath + "aapt", "remove", "-v",
					apkGameDataPath, "classes.dex" });
			if (activeProc.exitValue() != 0) {
				System.err
						.print("\nError while trying to clean GameData APK (classes.dex):\n"
								+ output);
				return false;
			}

			System.out.print("\nRemoving resources.arsc file...");
			output = execCmd(true, new String[] {
					androidPlatformToolsPath + "aapt", "remove", "-v",
					apkGameDataPath, "resources.arsc" });
			if (activeProc.exitValue() != 0) {
				System.err
						.print("\nError while trying to clean GameData APK (resources.arsc):\n"
								+ output);
				return false;
			}

			System.out.print("\nRemoving res folder...");
			output = execCmd(true, new String[] { "java", "-jar",
					toolsPath + "aaptExtTool.jar", androidPlatformToolsPath,
					"-removefolder", apkGameDataPath, "res", "assets/bin/",
					"assets/libs/", "lib/", "AndroidManifest.xml", "META-INF" });

			if (activeProc.exitValue() != 0) {
				System.err
						.print("\nError while trying to clean GameData APK (res/ folder):\n"
								+ output);
				return false;
			}
		}

		return true;
	}

	public static void repackageApk(int apkType) {
		String output = "";
		String outApkPath = "";
		String inApkPath = getPathApkType(apkType);

		if (apkType == 2)
			outApkPath = inputApkPath + inputApkNameNoExt + "_Installer"
					+ "_tmp.apk";
		else if (apkType == 3) {
			outApkPath = inputApkPath + inputApkNameNoExt + "_gameData"
					+ "_tmp.apk";
		}

		execCmd(false, new String[] { androidSdkToolsPath + "apkbuilder",
				outApkPath, "-v", "-z", inApkPath });
		getActiveProcessConsoleOutput(true);

		new File(inApkPath).delete();

		new File(outApkPath).renameTo(new File(inApkPath));
	}

	public static boolean prepareUnityStagingAreaFolders() {
		pathStagingArea = unityProjectPath + "Temp" + File.separator
				+ "StagingArea" + File.separator;

		System.out.print("\nUnity StagingArea path = " + pathStagingArea);

		String pathExtraBin = pathStagingArea + "extrabin" + File.separator;
		String pathStagingAssetsData = pathStagingArea + "assets"
				+ File.separator + "bin" + File.separator + "Data"
				+ File.separator;
		try {
			FileUtils.forceMkdir(new File(pathExtraBin + "lib"));
			FileUtils.forceMkdir(new File(pathExtraBin + "tmp"));
		} catch (Exception ex) {
			System.err
					.print("\nError in prepareUnityStagingAreaFolders() -> couldn't create staging area folders:\n"
							+ ex.getMessage());
			ex.printStackTrace();
			return false;
		}

		try {
			FileUtils.copyFileToDirectory(new File(pathStagingAssetsData
					+ "splash.png"), new File(pathExtraBin + "tmp"));
			FileUtils.copyFileToDirectory(new File(pathStagingAssetsData
					+ "settings.xml"), new File(pathExtraBin + "tmp"));

			FileUtils.deleteQuietly(new File(pathStagingArea + "assets"
					+ File.separator));

			FileUtils.copyFileToDirectory(new File(pathExtraBin + "tmp"
					+ File.separator + "splash.png"), new File(
					pathStagingAssetsData));
			FileUtils.copyFileToDirectory(new File(pathExtraBin + "tmp"
					+ File.separator + "settings.xml"), new File(
					pathStagingAssetsData));

			FileUtils.deleteQuietly(new File(pathExtraBin + "tmp"
					+ File.separator));
		} catch (Exception ex) {
			System.err
					.print("\nError in prepareUnityStagingAreaFolders() -> couldn't clean the staging assets folder:\n"
							+ ex.getMessage());
			ex.printStackTrace();
			return false;
		}

		return true;
	}

	public static boolean copyInstallerResFolders() {
		System.out.print("\nInstaller res location: " + installerResPath);
		try {
			IOFileFilter filterMetaFiles = FileFilterUtils.suffixFileFilter(
					".meta", IOCase.INSENSITIVE);
			filterMetaFiles = FileFilterUtils.and(new IOFileFilter[] {
					FileFileFilter.FILE, filterMetaFiles });

			IOFileFilter filterDotFilesAndDirs = FileFilterUtils
					.and(new IOFileFilter[] {
							FileFilterUtils.or(new IOFileFilter[] {
									FileFileFilter.FILE,
									DirectoryFileFilter.DIRECTORY }),
							FileFilterUtils.prefixFileFilter(".") });

			IOFileFilter allFilters = FileFilterUtils.or(new IOFileFilter[] {
					filterMetaFiles, filterDotFilesAndDirs });

			IOFileFilter finalFilter = FileFilterUtils
					.notFileFilter(allFilters);

			FileUtils.copyDirectory(new File(installerResPath), new File(
					pathStagingArea + "res" + File.separator), finalFilter);
		} catch (Exception ex) {
			System.err
					.print("\nError in copyInstallerResFolders(...) -> couldn't copy the Installer res folder:\n"
							+ ex.getMessage());
			ex.printStackTrace();
			return false;
		}

		return true;
	}

	public static boolean generateAndCompileRClass() {
		try {
			try {
				FileUtils.cleanDirectory(new File(pathStagingArea + "gen"
						+ File.separator));
			} catch (Exception ex) {
				System.err
						.print("\nError while trying to clean the gen/ folder:\n"
								+ ex.getMessage());
				ex.printStackTrace();
				return false;
			}

			System.out.print("\nGenerating R.java class...");
			String output = execCmd(true, new String[] {
					androidPlatformToolsPath + "aapt", "package", "-v", "-f",
					"-M", toolsPath + "AndroidManifest.xml", "-I",
					androidJarPath, "-S", pathStagingArea + "res", "-m", "-J",
					pathStagingArea + "gen", "--non-constant-id" });

			if (activeProc.exitValue() != 0) {
				System.err.print("\nError while generating R.java class:\n"
						+ output);
				return false;
			}

			Collection genFolderList = FileUtils.listFiles(new File(
					pathStagingArea + "gen"), new String[] { "java" }, true);
			String genFilePath = ((File) genFolderList.iterator().next())
					.getPath();

			System.out.print("\nCompiling R.java class...");
			output = execCmd(true, new String[] { "javac", genFilePath });
			if (activeProc.exitValue() != 0) {
				System.err
						.print("\nError while trying to compile R.java class:\n"
								+ output);
				return false;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.err
					.print("\nUnexpected error in generateAndCompileRClass():\n"
							+ ex.getMessage());
			return false;
		}

		return true;
	}
	
	public static boolean copyInstallerClasses() {
		String stagingAreaClassesPath = pathStagingArea
				+ "gen" + File.separator;
		System.out.print("\nInstaller classes location: "
				+ installerClassesPath);
		System.out.print("\nStaging area classes location: "
				+ stagingAreaClassesPath);
		try {
			IOFileFilter classFileFilter = FileFilterUtils
					.or(new IOFileFilter[] { FileFileFilter.FILE,
							FileFilterUtils.suffixFileFilter(".class") });
			IOFileFilter rClassesFilter = FileFilterUtils
					.notFileFilter(FileFilterUtils.or(new IOFileFilter[] {
							FileFilterUtils.nameFileFilter("R.class"),
							FileFilterUtils.nameFileFilter("BuildConfig.class"),
							FileFilterUtils.prefixFileFilter("R$") }));
			IOFileFilter filter = FileFilterUtils.or(new IOFileFilter[] {
					FileFilterUtils.and(new IOFileFilter[] { classFileFilter,
							rClassesFilter }), DirectoryFileFilter.DIRECTORY });

			Iterator<File> files = FileUtils.listFiles(
					new File(installerClassesPath), filter,
					DirectoryFileFilter.INSTANCE).iterator();
			while (files.hasNext()) {
				System.out.println("\n" + files.next().getAbsolutePath());
			}
			FileUtils.copyDirectory(new File(installerClassesPath), new File(
					stagingAreaClassesPath), filter);
		} catch (Exception ex) {
			System.err
					.print("\nError in copyInstallerResFolders(...) -> couldn't copy the Installer classes folder:\n"
							+ ex.getMessage());
			ex.printStackTrace();
			return false;
		}

		return true;
	}	

	public static boolean generateDexFile() {
		try {
			System.out.print("\nCurr dir: " + new File(".").getAbsolutePath());

			String dexOutPath = pathStagingArea + "extrabin" + File.separator
					+ "classes.dex";
			String dexInputPath1 = pathStagingArea + "plugins";
			String dexInputPath2 = pathStagingArea + "gen";
			String dexInputPath3 = pathStagingArea + "bin";

			execCmd(false, new String[] { androidPlatformToolsPath + "dx",
					"--dex", "--verbose", "--output=" + dexOutPath,
					dexInputPath1, dexInputPath2, dexInputPath3 });

			getActiveProcessConsoleOutput(false);
			getActiveProcessErrorOutput(true);

			System.out.print("\nCurr dir: " + new File(".").getAbsolutePath());

			if (activeProc.exitValue() != 0) {
				System.err.print("\nError while trying to generate DEX file!");
				return false;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.err.print("\nUnexpected error in generateDexFile():\n"
					+ ex.getMessage());
			return false;
		}

		return true;
	}

	public static boolean prepareUnityNativeLibsForApk() {
		try {
			Collection<File> nativeLibs = FileUtils.listFiles(new File(
					pathStagingArea + "libs"), new String[] { "so" }, true);
			if (nativeLibs.size() < 2) {
				System.err
						.print("\nError! Found less than 2 Unity libs!\nFailed in prepareUnityNativeLibsForApk()");
				System.err.print("\nLooked for native *.so libs in : "
						+ pathStagingArea + "libs\n");
				return false;
			}

			FileUtils.cleanDirectory(new File(pathStagingArea + "extrabin"
					+ File.separator + "lib"));

			for (File f : nativeLibs) {
				System.out.print("\nCopying Unity native lib:" + f.getPath());
				String libPathNoEndSep = FilenameUtils.getPathNoEndSeparator(f
						.getPath());
				String libFolderName = libPathNoEndSep
						.substring(
								FilenameUtils
										.indexOfLastSeparator(libPathNoEndSep) + 1)
						.trim();
				File destDir = new File(pathStagingArea + "extrabin"
						+ File.separator + "lib" + File.separator
						+ libFolderName);

				FileUtils.copyFileToDirectory(f, destDir);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.err
					.print("\nUnexpected error in prepareUnityNativeLibsForApk():\n"
							+ ex.getMessage());
			return false;
		}
		return true;
	}

	public static boolean createUnalignedInstallerApk(boolean debugModeOn) {
		try {
			String output;
			if (debugModeOn) {
				output = execCmd(true, new String[] {
						androidPlatformToolsPath + "aapt",
						"package",
						"-v",
						"-f",
						"-M",
						toolsPath + "AndroidManifest.xml",
						"--debug-mode",
						"--non-constant-id",
						"-F",
						pathStagingArea + "installer_unaligned.apk",
						"-I",
						androidJarPath,
						"-S",
						pathStagingArea + "res",
						"-m",
						"-J",
						pathStagingArea + "gen",
						"-P",
						pathStagingArea + "bin" + File.separator
								+ "resources.arsc", "-A",
						pathStagingArea + "assets",
						pathStagingArea + "extrabin" });
			} else {
				output = execCmd(true, new String[] {
						androidPlatformToolsPath + "aapt",
						"package",
						"-v",
						"-f",
						"-M",
						toolsPath + "AndroidManifest.xml",
						"--non-constant-id",
						"-F",
						pathStagingArea + "installer_unaligned.apk",
						"-I",
						androidJarPath,
						"-S",
						pathStagingArea + "res",
						"-m",
						"-J",
						pathStagingArea + "gen",
						"-P",
						pathStagingArea + "bin" + File.separator
								+ "resources.arsc", "-A",
						pathStagingArea + "assets",
						pathStagingArea + "extrabin" });
			}

			System.out.print("\n" + output);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.err
					.print("\nUnexpected error in createUnalignedInstallerApk():\n"
							+ ex.getMessage());
			return false;
		}

		return true;
	}

	public static boolean signApkWithDebugKey(String apkFilePath) {
		execCmd(false, new String[] {
				"jarsigner",
				"-verbose",
				"-keystore",
				FileUtils.getUserDirectoryPath() + File.separator + ".android"
						+ File.separator + "debug.keystore", "-storepass",
				"android", "-keypass", "android", apkFilePath,
				"androiddebugkey" });

		getActiveProcessConsoleOutput(true);

		if (activeProc.exitValue() != 0) {
			System.err
					.print("\nError while trying to sign APK with debug certificate file! Apk: "
							+ apkFilePath);
			return false;
		}

		return true;
	}

	public static boolean signApkWithUserKey(String apkFilePath,
			String keystoreFile, String keystorePass, String aliasName,
			String aliasPass) {
		execCmd(false, new String[] { "jarsigner", "-verbose", "-keystore",
				keystoreFile, "-storepass", keystorePass, "-keypass",
				aliasPass, apkFilePath, aliasName });

		getActiveProcessConsoleOutput(true);

		if (activeProc.exitValue() != 0) {
			System.err
					.print("\nError while trying to sign APK with use certificate file: "
							+ keystoreFile);
			System.err.print("\nAPK: " + apkFilePath);
			return false;
		}

		return true;
	}

	public static boolean outputUnalignedInstallerApk() {
		try {
			FileUtils.copyFile(new File(pathStagingArea
					+ "installer_unaligned.apk"), new File(apkInstallerPath));
		} catch (Exception ex) {
			ex.printStackTrace();
			System.err
					.print("\nUnexpected error in outputUnalignedInstallerApk():\n"
							+ ex.getMessage());
			return false;
		}

		return true;
	}

	public static boolean checkZipAlignStatusForApk(int apkType,
			boolean allowRepackage, boolean allowApkAlignModify) {
		String outApkPath = "";

		if (apkType == 2)
			outApkPath = inputApkPath + inputApkNameNoExt + "_Installer"
					+ "_tmp.apk";
		else if (apkType == 3)
			outApkPath = inputApkPath + inputApkNameNoExt + "_gameData"
					+ "_tmp.apk";
		String output;
		if (allowApkAlignModify) {
			output = execCmd(true, new String[] {
					androidSdkToolsPath + "zipalign", "-v", "4",
					getPathApkType(apkType), outApkPath });

			new File(getPathApkType(apkType)).delete();

			new File(outApkPath).renameTo(new File(getPathApkType(apkType)));
		} else {
			output = execCmd(true, new String[] {
					androidSdkToolsPath + "zipalign", "-c", "-v", "4",
					getPathApkType(apkType) });
		}
		System.out.print("\n" + output);

		if ((activeProc.exitValue() != 0) && (allowRepackage)) {
			System.out.print("\nVerification for " + getStrApkType(apkType)
					+ " APK FAILED!");
			System.out
					.print("\nTrying to repackage APK with apkbuilder tool...");
			repackageApk(apkType);

			System.out
					.print("\nFinished repackaging APK with apkbuilder tool...");

			System.out.print("\n\nRe-checking " + getStrApkType(apkType)
					+ " APK...");
			boolean recheckPassed = checkZipAlignStatusForApk(apkType, false,
					true);

			if (!recheckPassed)
				System.err
						.print("\nWARNING: "
								+ getStrApkType(apkType)
								+ " APK zipalign re-check failed even after APK repackage!");
		} else if (activeProc.exitValue() != 0) {
			System.err.print("\nERROR: zipalign failed with exit code:"
					+ activeProc.exitValue());
			return false;
		}

		return true;
	}

	public static final int getOSType() {
		if (System.getProperty("os.name").trim().toLowerCase()
				.contains("windows"))
			return 0;
		if (System.getProperty("os.name").trim().toLowerCase().contains("mac")) {
			return 1;
		}

		return -1;
	}

	public static String getUnityAndroidSDKRootSetting() {
		String macParseKey = "<key>AndroidSdkRoot</key>";
		String unityMacCfgFileName = "com.unity3d.UnityEditor3.x.plist";
		String unityWinCfgFileName = "";
		String unityPreferencesFilePath = "";

		String androidSdkRoot = "";

		if (getOSType() == 1) {
			unityPreferencesFilePath = System.getProperty("user.home")
					+ File.separator + "Library" + File.separator
					+ "Preferences" + File.separator;

			File f = new File(unityPreferencesFilePath + unityMacCfgFileName);
			if (!f.exists()) {
				System.out.print("\nUnity 3.x config file NOT found at: "
						+ unityPreferencesFilePath + unityMacCfgFileName);
				return "";
			}

			File tempCfgFile = null;
			String tempCfgFilePath = "";
			try {
				tempCfgFile = File.createTempFile("unity", "config");
				tempCfgFilePath = tempCfgFile.getAbsolutePath();
				tempCfgFile.delete();
			} catch (Exception ex) {
				System.err
						.print("Error while trying to create temp unity config file!");
				return "";
			}

			String plutilOutput = execCmd(true, new String[] { "plutil",
					"-convert", "xml1",
					unityPreferencesFilePath + unityMacCfgFileName, "-o",
					tempCfgFilePath });

			if (activeProc.exitValue() != 0) {
				System.err
						.print("Error while trying to convert binary plist Unity file to readable xml format.");
				return "";
			}

			f = new File(tempCfgFilePath);
			try {
				FileInputStream fIn = new FileInputStream(f);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(fIn));
				String strLine = "";
				while ((strLine = reader.readLine()) != null) {
					int keyIndex = strLine.indexOf(macParseKey);
					if (keyIndex >= 0) {
						strLine = reader.readLine();
						int idxStringTagStart = strLine.indexOf("<string>")
								+ "<string>".length();
						int idxStringTagEnd = strLine.lastIndexOf("</string>");
						androidSdkRoot = strLine.substring(idxStringTagStart,
								idxStringTagEnd).trim();
						break;
					}
				}
				reader.close();
				fIn.close();

				f.delete();
			} catch (Exception ex) {
				ex.printStackTrace();
				return "";
			}
		} else if (getOSType() != 0)
			;
		return androidSdkRoot;
	}

	public static void saveResPathToCfgFile(String resPath) {
		File f = new File(toolsPath + "builderTool.cfg");
		try {
			if (f.exists()) {
				f.delete();
			}

			PrintWriter writer = new PrintWriter(new FileWriter(f));
			writer.println(resPath);

			writer.println(toolsPath);
			writer.close();
		} catch (Exception ex) {
			System.err
					.print("\nError while trying to rewrite builder tool config file: "
							+ f.getAbsolutePath());
		}
	}

	public static String loadResPathFromCfgFile() {
		File f = new File(toolsPath + "builderTool.cfg");
		String resPath = "";
		String savedToolsPath = "";
		try {
			if (!f.exists()) {
				return "";
			}

			BufferedReader reader = new BufferedReader(new FileReader(f));
			resPath = reader.readLine();
			savedToolsPath = reader.readLine();

			reader.close();
		} catch (Exception ex) {
			System.err
					.print("\nError while trying to rewrite builder tool config file: "
							+ f.getAbsolutePath());
		}

		if (!savedToolsPath.trim().equals(toolsPath)) {
			System.err
					.print("\nSaved tools path is different from cuurent one!!!");
			System.err.print("\nSaved:" + savedToolsPath);
			System.err.print("\nCurrent:" + toolsPath);
			return "";
		}

		return resPath;
	}
}