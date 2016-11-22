package github.grails.example

import grails.converters.JSON
import groovy.json.JsonSlurper
import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicNameValuePair
import org.grails.web.json.JSONObject
import org.kohsuke.github.GHAuthorization
import org.kohsuke.github.GitHub

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

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

        redirect(url:"https://github.com/login/oauth/authorize?client_id=${clientToken}&scope=user")
//        redirect(url:"https://github.com/login/oauth/authorize?client_id=${clientToken}")


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
        println "attempting to authenticate "
        def code = params.code

        def clientToken = grailsApplication.config.getProperty("github.client.token")
        def clientSecret = grailsApplication.config.getProperty("github.client.secret")

//        String url = "https://github.com/login/oauth/access_token?client_token=${clientToken}&client_secret=${clientSecret}&code=${code}"
        // https://developer.github.com/v3/oauth/#2-github-redirects-back-to-your-site
        // TODO: 1 post to the client to get the acces token
//        def client = new restclient("https://github.com/login/oauth/access_token")
         def parameterMap = [
                client_id: clientToken
                ,client_secret : clientSecret
                , code: code
//                 , redirect_uri: "http://35.164.186.194:8080/"
        ]

        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        parameterMap.each {
            urlParameters.add(new BasicNameValuePair(it.key,it.value))
        }



//        def jsonSlurper = new JsonSlurper()

//        def jsonObject = jsonSlurper.parse(arguments)

//        DefaultHttpClient httpClient = new DefaultHttpClient();
//        HttpClientBuilder httpCliAentBuilder = new HttpClientBuilder();
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost postRequest = new HttpPost("https://github.com/login/oauth/access_token")
//        postRequest.addHeader("User-Agent", "TermGenie/1.0");
        postRequest.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.5; rv:10.0.1) Gecko/20100101 Firefox/10.0.1 SeaMonkey/2.7.1");
        postRequest.addHeader("Accept","application/json")
        postRequest.addHeader("Accept","application/xml")

//        String jsonObjectString = jsonObject.toString()
//        jsonObjectString = jsonObjectString.replaceAll('\"','\\\"')
//        println "posting '${jsonObjectString}'"
        println "posting '${urlParameters}'"

//        StringEntity input = new StringEntity(jsonObjectString)
//        postRequest.setEntity(input);
        postRequest.setEntity(new UrlEncodedFormEntity(urlParameters));

//        postRequest.addHeader("User-Agent", USER_AGENT);
        HttpResponse response = httpClient.execute(postRequest);

        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        println "final result ${result}"

        flash.message = result


        // TODO:  use the token to acces the API
        // the final get using the access_token in result
        // https://developer.github.com/v3/oauth/#3-use-the-access-token-to-access-the-api
//        curl -H "Authorization: token OAUTH-TOKEN" https://api.github.com/user


//        GitHub.createOrGetAuth(clientToken,clientSecret)

        respond view: "index",GithubUser.list(params), model:[githubUserCount: GithubUser.count()]
    }
}
