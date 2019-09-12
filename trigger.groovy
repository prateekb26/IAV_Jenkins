import groovy.util.XmlSlurper
import java.util.Map
import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.console.HyperlinkNote


EnvVars envVars = build.getEnvironment(listener);

filename = envVars.get('JOB_URL')

def currentBuild = Thread.currentThread().executable
def currentParams = currentBuild.getAction(ParametersAction.class)
def url
url = filename + "parambuild/?"
for (ParameterValue p in currentParams) {
    url=url+ p.name + "=" + p.value + "&"
}

print url + "\n"
def pa = new ParametersAction([
        new StringParameterValue('Remote.URL', url)
])
currentBuild.addAction(pa)


envVars.put ('Remote.URL','fsdfgasdf')
Jenkins.getInstance().save()


