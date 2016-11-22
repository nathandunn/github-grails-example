package github.grails.example

import org.kohsuke.github.GitHub

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class GithubUserController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond GithubUser.list(params), model:[githubUserCount: GithubUser.count()]
    }

    def show(GithubUser githubUser) {
        respond githubUser
    }

    def create() {
        respond new GithubUser(params)
    }

    @Transactional
    def save(GithubUser githubUser) {
        if (githubUser == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        if (githubUser.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond githubUser.errors, view:'create'
            return
        }

        githubUser.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'githubUser.label', default: 'GithubUser'), githubUser.id])
                redirect githubUser
            }
            '*' { respond githubUser, [status: CREATED] }
        }
    }

    def edit(GithubUser githubUser) {
        respond githubUser
    }

    @Transactional
    def update(GithubUser githubUser) {
        if (githubUser == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        if (githubUser.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond githubUser.errors, view:'edit'
            return
        }

        githubUser.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'githubUser.label', default: 'GithubUser'), githubUser.id])
                redirect githubUser
            }
            '*'{ respond githubUser, [status: OK] }
        }
    }

    @Transactional
    def delete(GithubUser githubUser) {

        if (githubUser == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        githubUser.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'githubUser.label', default: 'GithubUser'), githubUser.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'githubUser.label', default: 'GithubUser'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }

    def authenticateUser(GithubUser githubUser){

        def clientToken = grailsApplication.config.getProperty("github.client_token")
        println "client token: '${clientToken}'"

        redirect(url:"https://github.com/login/oauth/authorize?client_id=${clientToken}")


//        URL url = new URL("https://github.com/login/oauth/authorize?client_id=${clientToken}")
//        url.text

//        GitHub github = GitHub.connectUsingOAuth(githubUser.token1)
//
//        def userList = [
//                "nathandunn"
//                ,"cjmungall"
//                ,"kltm"
//                ,"selewis"
//        ]
//        def users = []
//        userList.each {
//            try {
//                def user = github.getUser(it)
//                if(user){
//                    users << user
//                }
//            } catch (e) {
//                println e
//            }
//        }
////        GitHub github = GitHub.connect("ndunn@me.com",githubUser.token1)
//
//        flash.message = "Authenticating user ${githubUser.username} using the Oauth valid ${github.credentialValid}"
//
//        println "users ${users}"
//
//        respond githubUser, view: "show",model:[users:users]
    }

    def githubAuthenticate(){
        def p = params
        def r = response

        println "params: ${p}"
        println "response : ${r}"

        redirect("/")
    }
}
