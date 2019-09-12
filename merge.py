#!/usr/bin/python
# -*- coding: utf-8 -*-

"""
Created on Wed Feb 27 14:18:31 2019

@author: pbhatnag
"""

import re
import sys

# Sample components.txt
# ASL-Messages
# ASL-Phone
# ASL-PhoneConnectionManager
# AppAdapter-Message
# AppAdapter-Phone
# AppAdapter-PhoneConnectionManager
# AppAdapter-ConnectivityWlan
# AppAdapter-DigitalAssistant
# AppAdapter-Online
# AppAdapter-UserManagement
# AppAdapter-UserManagementControlCenter
# ASL-UserManagement
# ASL-Sound
# AppAdapter-Sound

# The version number from command line Arguments

tagPart = {}
fixedSourcePart = 'https://cns3-certs.joomo.de/svn'
fixedPart = 'https://cns3-certs.joomo.de/svn/cns3extern/hmi/tags/teams/'
componentPart = {}
folderPart = {}
sourcePath = {}
targetPath = {}
componentList = []
mPath = {}
pom = {}
email = {}
newFlag = {}
pomLevel = {}

def parseConfig():
    component = ''

    # f = open("CNS3.txt", "r")

    f = open(sys.argv[2], 'r')
    for x in f:
        #To ignore comments 
        if (x.startswith('#', 0) != 1) and (re.match(r'\w', x)):
            j = 1
            for i in x.split('|'):
                if j == 1:
                    component = i.strip()
                    componentList.append(component)
                elif j == 2:
                    mPath[component] = i
                elif j == 3:
                    pom[component] = i
                elif j == 4:
                    email[component] = i
                elif j == 5:
                    pomLevel[component] = i.strip()
                elif j == 6:
                    newFlag[component] = i.strip()
                j = j + 1


    #printConfig()

def printConfig():
    for (x, y) in mPath.items():
        print (x, y)
    for (x, y) in pom.items():
        print (x, y)
    for (x, y) in email.items():
        print (x, y)
    for (x, y) in newFlag.items():
        print (x, y)
    for (x, y) in pomLevel.items():
        print (x, y)

# This function extract the component wise tag number from O drive and populate arrays with component
# such as for ASL-Sound = ASL-Sound_2019.x.x

def get_tag():

    # inputFilename = "O:\MIB-WebDAV\WebDAV\\" + version + "\ReleaseNotes-baseline-info.txt"
    sstring =""
    inputFilename = sys.argv[1]
    t = ""
    # inputFilename = "O:\MIB-WebDAV\WebDAV\H40.73.65\ReleaseNotes-baseline-info.txt"

    for component in componentList:
        f = open(inputFilename, 'r')
        for x in f:
            if component in x and newFlag[component] in x:   
                if 'ASL' in component and 'ASL-Tooling' not in component:
                    folderPart[component] = 'ASL'
                    sstring = folderPart[component] + '/' + component
                elif 'AppAdapter' in component and 'ASL-Tooling' not in component:
                    folderPart[component] = 'AppAdapter'
                    sstring = folderPart[component] + '/' + component
                else:
                    folderPart[component] = ''
                    sstring = component
                x = x.strip()
                b = re.search(r"(" + sstring + "\s+(.*)(\s:)\s(.*))", x)
                #print b.groups
                if b:
                    if 'mib2' in b.group(2):
                        t = re.sub('mib2', 'cns3extern', b.group(2))
                    elif 'mibextern' in b.group(2):
                        t = re.sub('mibextern', 'cns3extern', b.group(2))
                    if 'new ' in t:
                        t = re.sub('new ', '', t)
                    tagPart[component] = t


def form_path():
    for c in componentList:
        if mPath[c].rfind("http") != -1 :
            path=mPath[c]
        else:
            path = fixedPart.replace('tags', 'branches') + c \
                + '/release-branches/' + mPath[c] + '/' + folderPart[c] \
                + '/' + c
        targetPath[c] = path
        i = c
        if i == 'AppAdapter-Phone':
            i = 'Appadapter-Phone'
        if c in tagPart:
            path = fixedSourcePart + tagPart[c]
        else:
            path = 'No New code to Merge'
            targetPath[c] = path
        sourcePath[c] = path


# After extracting tag this function makes URL to merge such as
# https://cns3-token.joomo.de/svn/cns3extern/hmi/tags/teams/ASL-Sound/releases/ASL-Sound_2019.8.0/ASL/

def prepareSourcePath():
    parseConfig()
    #printConfig()
    get_tag()
    form_path()


def run():
    prepareSourcePath()


run()
for component in componentList:
    print component + '#' + sourcePath[component] + '#' \
        + targetPath[component] + '#' + pom[component] + '#' \
        + email[component] + '#' + pomLevel[component] +  '\n'
