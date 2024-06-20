#!groovy​

// Jenkinsfile IC.
// Utilisé pour la construction
//c10x version: 03.01.02

/**
 * CONFIGURATION JENKINS !
 *
 * Dans la configuration "Pipeline"
 * Spécifier ${gitlabBranch} dans [ Branches to build ->	Branch Specifier (blank for 'any') : ______ ]
 *
 * [ Branches to build -> Branch Specifier (blank for 'any') : ${gitlabBranch} ]
 */

// ####################################################################################
// ## CONSTANTES DU JOB
// ####################################################################################

/**
 * Branche principale, sur laquelle l'analyse sonar est lancée sans paramètre.
 * Voir https://docs.sonarqube.org/latest/branches/overview/
 **/
String BRANCHE_PRINCIPALE = 'main'

//####################################################################################
//## PARAMETRES DEPUIS Jenkins
//####################################################################################

/** Indique que l'on doit réaliser une release */
Boolean isRelease = utils.readBoolean(params.IS_RELEASE, false)

/** Version a release */
String releaseVersion = params.RELEASE_VERSION

/** Prochaine version de developpement */
String nextDevVersion = params.NEXT_DEV_VERSION + "-SNAPSHOT"

//####################################################################################
//## VARIABLES DU JOB
//####################################################################################

/** branch en cours de construction */
String currentBranch

/** branche cible dans le cas d'une MR */
String targetBranch

/** Version extraite du POM */
String versionApp

//####################################################################################
//## PIPELINE
//####################################################################################

echo "## PARAMETRES DU BUILD #######"
echo "$params"
echo "## / PARAMETRES DU BUILD #######"

def maintainers = "hugo.schindler@rte-france.com,viktor.terrier@rte-france.com"

// Execution sur le noeud de build de la plateforme Devin
node('build') {
	try{

		// Contrôle des parametres
		if (isRelease) {
			if(params.RELEASE_VERSION == null || params.RELEASE_VERSION == ''){
				error('Aucune version selectionnée. RELEASE_VERSION ne doit pas être vide')
			}
			if(params.NEXT_DEV_VERSION == null || params.NEXT_DEV_VERSION == ''){
				error('Aucune version selectionnée. NEXT_DEV_VERSION ne doit pas être vide')
			}
		}

		stageDevin ('🦊 Checkout') {
			// On force la suppression du workspace pour garantir de travailler sur un projet git propre
			// car on va commiter des choses dessus. (exemple: on ne veut pas repousser d'anciens Tags qui ont peut-être été supprimés de GitLab)
			cleanWs()

			// checkout du projet et récupération de la branche courante
			def result = gitCheckout {}
			currentBranch = "${result['GIT_BRANCH']}"
			// cible pour sonar. Alimenté uniquement dans le cas des MR. Par défaut BRANCHE_PRINCIPALE
			targetBranch = result['GIT_TARGET_BRANCH'] ?: BRANCHE_PRINCIPALE

			def pom = readMavenPom file: "pom.xml"
			versionApp = pom.version

			if(isRelease) {
				versionApp = releaseVersion
				utils.appendBuildDescription("🚀 release !", true)
			}

			utils.appendBuildDescription("🏺 jar: <b>${versionApp}</b>", true)
			utils.appendBuildDescription("🦊 git: <b>${result['GIT_INFO']}</b>", true)
		}

		stageDevin ('🏭 Build mvn') {
			echo "Construction de la version : ${versionApp}, à partir de (tag/branch) : ${currentBranch}"
			if (isRelease) {
				mvn {
					jdk = 'openjdk17'
					goal = 'versions:set'
					options = "-DnewVersion=${versionApp}"
				}
			}
			mvnPackage {
				jdk = 'openjdk17'
			}
		}

		stageDevin ('⚗️ Unit Test'){
			echo "⚗️Lancement des tests unitaire"
			junitTest{
				jdk = 'openjdk17'
				ignoreFailure = false
			}
		}

		stageDevin ('🔬 Code Quality') {
			echo "🔬 Analyse sonar du code"
			sonar {
				jdk = 'openjdk17'
				branch = currentBranch
			}
		}

        stageDevin ('🚚 Publication') {
            echo "🚚 Publication NEXUS"
            sh "touch .skip-publish-junit-results" // Temporary fix to disable double test report
            deployArtefact {
                repoReleaseName = 'algoleague-releases'
                repoSnapshotName = 'algoleague-snapshots'
                jdk = 'openjdk17'
            }
        }

		if (isRelease) {

			stageDevin ('🏷️Tag') {
				echo "🏷️ Creation du tag de release"
				String msg = "Tag Jenkins ${versionApp}\n\n ${env.BUILD_URL}"
				gitCommit{
					commentaire = "Jenkins Release: version $versionApp"
				}
				gitTag{
					tagName = versionApp
					doitPush = false
					tagMsg = msg
				}
			}

			stageDevin ("📝 Change POM version") {
				mvn {
					goal = 'versions:set'
					options = "-DnewVersion=${nextDevVersion}"
				}
				gitCommit{
					commentaire = "Jenkins Release: prepare next dev version: $nextDevVersion"
				}
				gitPush{}
			}
		}

	} catch (e) {
		notify{
			to = maintainers
			errorMsg = e.toString()
		}
		throw e
	} finally {
		echo "🗑️ Nettoyage du workspace"
		cleanWs()
	}
}
