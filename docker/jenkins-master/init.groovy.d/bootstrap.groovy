import jenkins.model.Jenkins
import hudson.model.FreeStyleProject
import javaposse.jobdsl.plugin.ExecuteDslScripts

def directory = '/opt/illumos-ci'
def name = 'bootstrap'

def job = Jenkins.instance.getItem(name)
if (job == null)
    job = Jenkins.instance.createProject(FreeStyleProject, name)
job.displayName = name

def file = new File("${directory}/seed_job.groovy")
def scripts = new ExecuteDslScripts(file.text)

job.buildersList.clear()
job.buildersList.add(scripts)
job.save()

// vim: tabstop=4 shiftwidth=4 expandtab textwidth=112 colorcolumn=120
