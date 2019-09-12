import groovy.util.XmlSlurper
import java.util.Map
import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.console.HyperlinkNote

def component
def source
def target
def words
def testPath
def emails
def pomLevel
def params = []

//test.txt is created by python and ant script and contains output of source url and target url
EnvVars envVars = build.getEnvironment(listener);

filename = envVars.get('WORKSPACE') + "\\test.txt";
println filename

//Fetch current thread to get current parameters for the jenkins job
def currentBuild = Thread.currentThread().executable
def currentParams = currentBuild.getAction(ParametersAction.class)

new File(filename).eachLine {  line ->
    if (line.trim().size() != 0) {
        words = line.split('#')
        component = words[0]
        println component
        source = words[1]
        println source
        target = words[2]
        println target
        testPath = words[3]
        println testPath
        emails = words[4]
        println emails
        pomLevel = words[5]
        println pomLevel
        params = []
        // if new flag is set , then there is no new code to merge then , we dont need to run jenkins jobs
        if (source != "No New code to Merge") {
            def job = Hudson.instance.getJob('MERGE 010 Merge Decider')
            //Extract all the current parameters to pass on to next job in pipeline
            for (ParameterValue p in currentParams) {
                switch (p.name) {
                    case 'Local.Path':
                        println "the local path is " + p.value
                        params.add(new StringParameterValue(p.name, p.value + "/" + component))
                        break
                    case 'skip.merge.conflicts':
                        println "the skip.merge.conflicts is " + p.value
                        params.add(new BooleanParameterValue(p.name, p.value))
                        break
                    case 'cleanup':
                        println "cleanup " + p.value
                        params.add(new BooleanParameterValue(p.name, p.value))
                        break
                    case 'commit':
                        println "commit " + p.value
                        params.add(new BooleanParameterValue(p.name, p.value))
                        break
                    case 'PRL.name':
                        println "PRL " + p.value
                        params.add(new StringParameterValue(p.name, p.value))
                        break
                    case 'H.Version':
                        println "PRL " + p.value
                        params.add(new StringParameterValue(p.name, p.value))
                        break
                    case 'POMs.url':
                        params.add(new StringParameterValue(p.name, p.value))
                }
            }

            params.add(new StringParameterValue('Merge.Source.Url', source))
            params.add(new StringParameterValue('Merge.Target.Url', target))
            params.add(new StringParameterValue('projects.to.test', testPath))
            params.add(new StringParameterValue('Email.List', emails))
            params.add(new StringParameterValue('Project.Name', component))
            params.add(new StringParameterValue('link.POMs.oneLevelDeeper', pomLevel))

            def paramsAction = new ParametersAction(params)
            def cause = new hudson.model.Cause.UpstreamCause(build)
            def causeAction = new hudson.model.CauseAction(cause)
            def future = job.scheduleBuild2(0, causeAction, paramsAction)
            println "Waiting for the completion of " + HyperlinkNote.encodeTo('/' + job.url, job.fullDisplayName)
            anotherBuild = future.get()
        }
    }
}
