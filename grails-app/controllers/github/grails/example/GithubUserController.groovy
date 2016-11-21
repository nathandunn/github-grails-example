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

        GitHub github = GitHub.connect(githubUser.username,githubUser.token1)


        flash.message = "Authenticating user ${githubUser.username} is valid ${github.credentialValid}"

        respond githubUser, view: "show"
    }
}
