package com.hmtmcse.ocb

import grails.web.servlet.mvc.GrailsParameterMap

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import org.hibernate.Hibernate


class ContactService {

    AuthenticationService authenticationService
    ContactDetailsService contactDetailsService

    def save(GrailsParameterMap params, HttpServletRequest request) {
        Contact contact = new Contact(params)
        contact.member = authenticationService.getMember()
        def response = AppUtil.saveResponse(false, contact)
        if (contact.validate()) {
            contact.save(flush: true)
            if (!contact.hasErrors()) {
                response.isSuccess = true
                contactDetailsService.createOrUpdateDetails(contact, params)
                uploadImage(contact, request)
            }
        }
        return response
    }

    def update(Contact contact, GrailsParameterMap params, HttpServletRequest request) {
        contact.properties = params
        def response = AppUtil.saveResponse(false, contact)

        if (contact.validate()) {
            contact.save(flush: true)
            if (!contact.hasErrors()) {
                response.isSuccess = true
                contactDetailsService.createOrUpdateDetails(contact, params)
                uploadImage(contact, request)
            }
        }
/*        contact.name = params.name
        contact.image = params.image

        def contactParams = ['mobile', 'phone', 'email', 'website', 'address']
        boolean hasFilledContactParams = contactParams.any { param ->
            params[param]?.trim()
        }


        println("Any filled contact params: " + hasFilledContactParams + "All params" + params)

        // Explicitly initialize contactGroup before any condition checks
        if (contact?.contactGroup) {
            Hibernate.initialize(contact.contactGroup)

        if (hasFilledContactParams) {

        } else {
            println("No changes made due to all contact parameters being empty.")
            response.message = "No changes made as all contact parameters are empty."
        }
        }*/

        return response
    }


    def get(Serializable id) {
        return Contact.get(id)
    }


    def list(GrailsParameterMap params) {
        params.max = params.max ?: GlobalConfig.itemsPerPage()
        List<Contact> contactList = Contact.createCriteria().list(params) {
            if (params?.colName && params?.colValue) {
                like(params.colName, "%" + params.colValue + "%")
            }
            if (!params.sort) {
                order("id", "desc")
            }
            eq("member", authenticationService.getMember())
        }
        return [list: contactList, count: contactList.totalCount]
    }


    def delete(Contact contact) {
        try {
            deleteImage(contact)
            contact.delete(flush: true)
        } catch (Exception e) {
            println(e.getMessage())
            return false
        }
        return true
    }


    def uploadImage(Contact contact, HttpServletRequest request) {
        if (request.getFile("contactImage") && !request.getFile("contactImage").filename.equals("")) {
            String image = FileUtil.uploadContactImage(contact.id, request.getFile("contactImage"))
            if (!image.equals("")) {
                contact.image = image
                contact.save(flush: true)
            }
        }
    }

    def deleteImage(Contact contact) {
        if (contact.image) {
            String imageDirectory = "${FileUtil.getRootPath()}contact-image/"
            println("IMAGEM A DELETAR:" + imageDirectory + contact.image)
            File imageFile = new File(imageDirectory, contact.id + "-" + contact.image)
            println("ARQUIVO A DELETAR:" + imageFile)
            if (imageFile.exists()) {
                imageFile.delete()
            }
        }
    }

}
