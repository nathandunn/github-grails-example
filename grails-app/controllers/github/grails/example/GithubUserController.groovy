package github.grails.example

import grails.transaction.Transactional
import groovy.json.JsonSlurper
import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicNameValuePair
import org.grails.web.json.JSONObject
import org.kohsuke.github.GHAuthorization
import org.kohsuke.github.GitHub

import static org.springframework.http.HttpStatus.*

@Transactional(readOnly = true)
class GithubUserController {

//    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE",githubAuthenticate:"POST"]

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

    def authenticateUser2(){
        def clientId = grailsApplication.config.getProperty("github.client.token")
        def clientSecret = grailsApplication.config.getProperty("github.client.secret")
        GitHub github = GitHub.connect();
        GHAuthorization authorization = github.createOrGetAuth(clientId,clientSecret,["user"],"test attempt to connect","")

    }

    def authenticateUser(GithubUser githubUser){

        def clientToken = grailsApplication.config.getProperty("github.client.token")
        println "client token: '${clientToken}'"

        redirect(url:"https://github.com/login/oauth/authorize?client_id=${clientToken}&scope=user:email")
    }

    private String getAccessToken(String code){
        def clientToken = grailsApplication.config.getProperty("github.client.token")
        def clientSecret = grailsApplication.config.getProperty("github.client.secret")

        // https://developer.github.com/v3/oauth/#2-github-redirects-back-to-your-site
        // TODO: 1 post to the client to get the acces token
        def parameterMap = [
                client_id: clientToken
                ,client_secret : clientSecret
                , code: code
        ]

        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        parameterMap.each {
            urlParameters.add(new BasicNameValuePair(it.key,it.value))
        }

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost postRequest = new HttpPost("https://github.com/login/oauth/access_token")
        postRequest.addHeader("User-Agent", "TermGenie/1.0");
        postRequest.addHeader("Accept","application/json")
        postRequest.addHeader("Accept","application/xml")

        println "posting '${urlParameters}'"

        postRequest.setEntity(new UrlEncodedFormEntity(urlParameters));

        HttpResponse response = httpClient.execute(postRequest);

        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        def jsonSlurper = new JsonSlurper()
        JSONObject jsonObject = jsonSlurper.parseText(result.toString()) as JSONObject
        String accessToken = jsonObject.access_token
        return accessToken
    }

    def githubAuthenticate(){
        println "attempting to authenticate "
        def code = params.code
        String accessToken = getAccessToken(code)

//        String text = new URL("https://github.com/api/v2/json/user/show?access_token=${accessToken}").text
        URL url = new URL("https://api.github.com/user?access_token=${accessToken}")
        def jsonObject = (new JsonSlurper()).parse(url) as JSONObject
        // is authenticated:
        boolean isAuthenticated = jsonObject.containsKey("email")
//        flash.message = "User details: " + text
//        flash.message = userString
        flash.message = "${jsonObject.login} (${jsonObject.email}) is authenticated: ${isAuthenticated} "

        respond view: "index",GithubUser.list(params), model:[githubUserCount: GithubUser.count()]
    }

}
