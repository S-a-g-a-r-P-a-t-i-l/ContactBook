package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository;

    // use for commonly add data in the
    @ModelAttribute
    public void addCommonData(Model model, Principal principal) {
        String username = principal.getName();
        User user = userRepository.getUserByUserName(username);
        model.addAttribute("user", user);
    }

    // use to get the dashboard
    @RequestMapping("/index")
    public String dashboard(Model model, Principal principal, HttpSession session) {
        model.addAttribute("title", "User Dashboard");
        session.setAttribute("con", null);
        return "normal/user_dashboard";
    }

    // open add contact form handler
    @GetMapping("/add-contact")
    public String OpenAddContactHandler(Model m, HttpSession session) {
        m.addAttribute("title", "Add Contact");
        session.setAttribute("con", true);
        m.addAttribute("contact", new Contact());
        return "normal/add_contact_form";
    }

    // handling the contact form data
    @PostMapping("/process-contact")
    public String processContact(@ModelAttribute("contact") Contact contact,
            @RequestParam("prfileImage") MultipartFile file, Principal principal, HttpSession session) {

        try {
            String username = principal.getName();
            User user = userRepository.getUserByUserName(username);
            // fetching the file and processing file

            if (file.isEmpty()) {
                System.out.println("file is empty");
                contact.setImage("contact.png");
            } else {
                contact.setImage(contact.getPhone() + file.getOriginalFilename());

                File saveFile = new ClassPathResource("/static/img").getFile();

                saveFile.renameTo(saveFile);
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                File oldFile = new File(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
                File rename = new File(
                        saveFile.getAbsolutePath() + File.separator + contact.getPhone() + file.getOriginalFilename());
                oldFile.renameTo(rename);
                // File rename = new File(
                // saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename() +
                // contact.getCid());

            }

            // saving the user
            contact.setUser(user);
            user.getContacts().add(contact);
            userRepository.save(user);
            System.out.println("Contact added successfully");
            session.setAttribute("message", new Message("Contact added successfully !! Add more...", "success"));
        } catch (Exception e) {
            session.setAttribute("message", new Message("Something wents wrong !!", "danger"));
            e.printStackTrace();
        }

        return "normal/add_contact_form";
    }

    // show contacts handler
    @GetMapping("/show-contacts/{page}")
    public String showContact(@PathVariable("page") Integer page, Model model, Principal principal) {
        model.addAttribute("title", "All Contacts");
        String name = principal.getName();
        User user = this.userRepository.getUserByUserName(name);
        // current page - n
        // contact per page - 5(page)
        Pageable pageable = PageRequest.of(page, 5);
        Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);

        model.addAttribute("contacts", contacts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPage", contacts.getTotalPages());
        return "normal/show_contacts";
    }

    @RequestMapping("/{cid}/contact")
    public String showContactDetails(@PathVariable("cid") Integer cid, Model m, Principal principal) {
        Optional<Contact> contacts = contactRepository.findById(cid);
        Contact contact = contacts.get();
        String name = principal.getName();
        User user = this.userRepository.getUserByUserName(name);
        if (user.getId() == contact.getUser().getId()) {
            m.addAttribute("title", contact.getName());
            m.addAttribute("contact", contact);
        }

        return "normal/contact_details";
    }

    // deleteing contact
    @RequestMapping("/delete/{cid}")
    public String deleteContact(@PathVariable("cid") Integer cid, Principal principal, HttpSession session) {
        Optional<Contact> contacts = contactRepository.findById(cid);
        Contact contact = contacts.get();
        String name = principal.getName();
        User user = userRepository.getUserByUserName(name);
        if (user.getId() == contact.getUser().getId()) {
            this.contactRepository.deleteById(cid);
            session.setAttribute("message",
                    new Message("Contact " + contact.getName() + " deleted successfully", "success"));
        }
        return "redirect:/user/show-contacts/0";
    }

    // updating contact form
    @PostMapping("/update-contact/{cid}")
    public String updateForm(@PathVariable("cid") Integer cid, Model m) {
        m.addAttribute("title", "Smart Contact Manager - Update Contact");
        Contact contact = this.contactRepository.findById(cid).get();
        m.addAttribute("contact", contact);

        return "/normal/update_form";
    }

    // update form handler
    @PostMapping("/process-update")
    public String updateHandler(@ModelAttribute("contact") Contact contact,
            @RequestParam("prfileImage") MultipartFile file, Model m, HttpSession session, Principal principal) {
        try {
            // old contact details
            Contact oldcontacatDetail = this.contactRepository.findById(contact.getCid()).get();
            if (!file.isEmpty()) {
                System.out.println(!oldcontacatDetail.getImage().equals("contact.png"));

                // delete old photo
                if (oldcontacatDetail.getImage() != "contact.png") {

                } else {
                    File deleteFile = new ClassPathResource("/static/img").getFile();
                    File file1 = new File(deleteFile, oldcontacatDetail.getImage());
                    file1.delete();
                }

                // update new photo
                File saveFile = new ClassPathResource("/static/img").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                File oldFile = new File(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
                File rename = new File(
                        saveFile.getAbsolutePath() + File.separator + contact.getPhone() + file.getOriginalFilename());
                oldFile.renameTo(rename);
                contact.setImage(contact.getPhone() + file.getOriginalFilename());
            } else {
                contact.setImage(contact.getPhone() + oldcontacatDetail.getImage());
            }
            User user = this.userRepository.getUserByUserName(principal.getName());
            user.getContacts().add(contact);
            contact.setUser(user);
            this.userRepository.save(user);
            session.setAttribute("message", new Message("Contact updated successfully", "success"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/user/" + contact.getCid() + "/contact";
    }

    // profile page handler
    @GetMapping("/profile")
    public String yourProfile(Model m, Principal principal) {
        m.addAttribute("title", "Smart Contact Manager - My Profile");
        String username = principal.getName();
        User user = this.userRepository.getUserByUserName(username);
        m.addAttribute("user", user);
        return "normal/profile";
    }
}
