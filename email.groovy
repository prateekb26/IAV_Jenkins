
import groovy.util.XmlSlurper
import java.util.Map
import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.console.HyperlinkNote
EnvVars envVars = build.getEnvironment(listener);

def currentBuild = Thread.currentThread().executable
def currentParams = currentBuild.getAction(ParametersAction.class)

def component
def project_to_test
def source
def target
def path
def pri
def pomUrl
def pomLevel
def pomPath

//Get all the parameter from the job
for (ParameterValue p in currentParams) {
    switch (p.name) {
        case 'Local.Path':
            path=p.value
            break
        case 'Merge.Source.Url':
            source=p.value.replaceAll('certs','token')
            break
        case 'Merge.Target.Url':
            target=p.value.replaceAll('certs','token')
            break
        case 'Project.Name':
            component=p.value
            break
        case 'PRL.name':
            pri=p.value
            break
        case 'POMs.url':
            pomUrl=p.value.replaceAll('certs','token')
            break
        case 'projects.to.test':
            project_to_test=p.value
            break
        case 'link.POMs.oneLevelDeeper':
            println "p.value=" + p.value
            pomLevel=p.value
            break
    }
}
//Prepare the local path to checkout , merge and test
def delPath = "D:/JenkinsAutoMerge/"
path = delPath + pri + "/" + component

//Create a new file to be executed for checkout and marge at current workspace
def newFile = new File(envVars.get('WORKSPACE') + "\\0_checkoutAndMerge_" + pri + "_" + component + ".ba")
newFile.createNewFile()
def filename = envVars.get('WORKSPACE') +  "\\0_checkoutAndMerge_" + pri + "_" + component + ".ba"

println "**************************************************************************************"
println "Generated 0_checkoutAndMerge file at"
println filename
println "**************************************************************************************"
newFile.write("\n")

pomPath = path + "\\..\\POMs"

def sourcePath = path + "/source"
def targetPath = path + "/target"

//Batch script to cleanup ,checkout POMs , source and target and later merge
newFile.append("echo deleting ..." + path + "\n")
newFile.append("rmdir /Q /S " + "\"" + path + "\"" + "\n")

newFile.append("svn co " + source + " " + sourcePath + " --ignore-externals \n")
newFile.append("svn co " + target + " " + targetPath + " --ignore-externals \n")
newFile.append("IF EXIST "+ "\""+ "" +pomPath + "\""+ " ( \n")
newFile.append("echo POMs are checkout hence switching - svn switch \n")
newFile.append("svn switch " + pomUrl + " " + pomPath + " --ignore-externals \n")
newFile.append(") ELSE ( \n")
newFile.append("echo POMs are not checkedout hence checking out \n")
newFile.append("svn co " + pomUrl + " " + pomPath + "\n")
newFile.append(") \n")
if (pomLevel) {
    def mlink = path + "/POMs"
    mlink=mlink.replaceAll('/','\\\\')
    def mtarget = path + "/../POMs"
    mtarget =mtarget .replaceAll('/','\\\\')
    newFile.append("mklink /J " + mlink + " " + mtarget + "\n")
    println "link.POMs.oneLevelDeeper was true " + pomPath
}


newFile.append("svn merge -r 0:HEAD " + sourcePath + " " + targetPath + "\n")
newFile.append("PAUSE")

//Create a new file to be executed for mvn install and test
newFile = new File(envVars.get('WORKSPACE') + "\\1_mvnInstalAndTest_" + pri + "_" + component + ".ba")
newFile.createNewFile()
filename = envVars.get('WORKSPACE') +  "\\1_mvnInstalAndTest_" + pri + "_" + component + ".ba"

println "**************************************************************************************"
println "Generated 1_mvnInstalAndTest file at"
println filename
println "**************************************************************************************"
newFile.write("\n")

//Batch script to mvn install and test along with error handling
switch(project_to_test) {
    case '-self-':
        newFile.append("call mvn3.bat install -f" + targetPath + "/pom.xml -Dmaven.test.skip=true -Dmaven.repo.local=" + path + "/.repo \n")
        newFile.append("IF NOT %ERRORLEVEL%==0 GOTO INSTALLATION_ERRORS \n")

        newFile.append("call mvn3.bat test -f" + targetPath + "/pom.xml -Dmaven.repo.local=" + path + "/.repo \n")
        newFile.append("IF NOT %ERRORLEVEL%==0 GOTO TESTING_ERRORS \n")
        break
    case '-aggregates-':
        newFile.append("call mvn3.bat install -f" + targetPath + "/" + "Aggregates/Install/pom.xml -Dmaven.test.skip=true -Dmaven.repo.local=" + path + "/.repo \n")
        newFile.append("IF NOT %ERRORLEVEL%==0 GOTO INSTALLATION_ERRORS \n")

        newFile.append("call mvn3.bat test -f" + targetPath + "/"  + "Aggregates/Test/pom.xml -Dmaven.repo.local=" + path + "/.repo \n")
        newFile.append("IF NOT %ERRORLEVEL%==0 GOTO TESTING_ERRORS \n")
        break
    default:
        for (i in project_to_test.split(',')) {
            newFile.append("call mvn3.bat install -f" + targetPath + "/" + i + "/pom.xml -Dmaven.test.skip=true -Dmaven.repo.local=" + path + "/.repo \n")
            newFile.append("IF NOT %ERRORLEVEL%==0 GOTO INSTALLATION_ERRORS \n")
        }
        for (i in project_to_test.split(',')) {
            newFile.append("call mvn3.bat test -f" + targetPath + "/" + i + "/pom.xml -Dmaven.repo.local=" + path + "/.repo \n")
            newFile.append("IF NOT %ERRORLEVEL%==0 GOTO TESTING_ERRORS \n")

        }
}


//Error Handling if mvn install or test fails
newFile.append(":INSTALLATION_ERRORS \n")
newFile.append("echo Process stops because of maven install errors \n")
newFile.append(":END \n")
newFile.append("GOTO PAUSE_LABEL \n")

newFile.append(":TESTING_ERRORS \n")
newFile.append("echo Process stops because of maven test errors \n")
newFile.append(":END \n")
newFile.append("GOTO PAUSE_LABEL \n")

newFile.append(":PAUSE_LABEL \n")
newFile.append("PAUSE \n")
newFile.append(":END \n")

