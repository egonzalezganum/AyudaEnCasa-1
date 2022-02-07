package com.ayudaencasa.app.controllers;

import com.ayudaencasa.app.criteria.PetWalkerCriteria;
import com.ayudaencasa.app.dto.input.CreatePetWalkerDTO;
import com.ayudaencasa.app.dto.input.SearchPetWalkerDTO;
import com.ayudaencasa.app.entities.PetWalker;
import com.ayudaencasa.app.exceptions.PetWalkerNotFoundException;
import com.ayudaencasa.app.services.PetWalkerService;
import com.ayudaencasa.app.services.S3Service;
import io.github.jhipster.service.filter.IntegerFilter;
import io.github.jhipster.service.filter.StringFilter;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Validated
@RequestMapping("/petwalker")
public class PetWalkerController {

    @Autowired
    private PetWalkerService petWalkerService;
    @Autowired
    private S3Service s3service;
    @Autowired
    private ModelMapper modelmap;

    @GetMapping("/create")
    public String registry(Model model, @RequestParam(required = false) String id) {
        if (id != null) {
            PetWalker petWalker = petWalkerService.findById(id);
            if (petWalker != null) {
                model.addAttribute("id", id);
                model.addAttribute("salary", petWalker.getSalary());
                model.addAttribute("petQuantity", petWalker.getPetQuantity());
            } else {
                return "redirect:/petwalker/list";
            }
        }
        return "petwalkerForm";
    }

    @PostMapping("/create")
    public String create(RedirectAttributes redirectat, CreatePetWalkerDTO inputPetWalker) {
        try {
            PetWalker petWalker = new PetWalker();
            System.out.println(inputPetWalker);
            petWalkerService.validated(inputPetWalker);
            modelmap.map(inputPetWalker, petWalker);
            if(inputPetWalker.getCv() != null && !inputPetWalker.getCv().isEmpty()){
                petWalker.setCurriculum(s3service.save(inputPetWalker.getCv()));
            }
            if (inputPetWalker.getWorkingHoursTo() != null) {
                petWalker.setHoursTo(inputPetWalker.getWorkingHoursTo());
            }
            if (inputPetWalker.getWorkingHoursFrom() != null) {
                petWalker.setHoursFrom(inputPetWalker.getWorkingHoursFrom());
            }
            if (inputPetWalker.getId() != null  && !inputPetWalker.getId().isEmpty()) {
                update(inputPetWalker.getId(), petWalker);
                redirectat.addFlashAttribute("success", "Se ha modificado con éxito en paseador de mascotas");
            } else {
                petWalkerService.create(petWalker);
                redirectat.addFlashAttribute("success", "Se ha registrado con éxito en paseador de mascotas");
            }
            return "redirect:/home";
        } catch (PetWalkerNotFoundException ex) {
            redirectat.addFlashAttribute("error", ex.getMessage());
            redirectat.addFlashAttribute("id", inputPetWalker.getId());
            redirectat.addFlashAttribute("salary", inputPetWalker.getSalary());
            redirectat.addFlashAttribute("petQuantity", inputPetWalker.getPetQuantity());
            redirectat.addFlashAttribute("workingHoursFrom", inputPetWalker.getWorkingHoursFrom());
            redirectat.addFlashAttribute("workingHoursTo", inputPetWalker.getWorkingHoursTo());
            return "redirect:/petwalker/create";
        }
    }

    @GetMapping("/list")
    public String findAll(Model model, @RequestParam(required = false, name="petwalkers") List<PetWalker> petWalkers) {
        if (petWalkers != null) {
            model.addAttribute("petwalkers", petWalkers);
        } else {
            model.addAttribute("petwalkers", petWalkerService.findAll());
        }
        return "petwalkerList";
    }

    @PostMapping("/list")
    public String findByFilter(SearchPetWalkerDTO searchPetWalker, RedirectAttributes rt) {
        if (searchPetWalker.getWorkingHoursTo() != null) {
            searchPetWalker.setHoursTo(searchPetWalker.getWorkingHoursTo());
        }
        if (searchPetWalker.getWorkingHoursFrom() != null) {
            searchPetWalker.setHoursFrom(searchPetWalker.getWorkingHoursFrom());
        }
        PetWalkerCriteria petWalkerCriteria = createCriteria(searchPetWalker);
        List<PetWalker> petWalkers = petWalkerService.findByCriteria(petWalkerCriteria);

        if (searchPetWalker.getDay() != null && !searchPetWalker.getDay().isEmpty()) {
            List<PetWalker> pet = new ArrayList<>();
            for (PetWalker petWalker : petWalkers) {
                for (String day : petWalker.getDays()) {
                    if (day.equalsIgnoreCase(searchPetWalker.getDay())) {
                        pet.add(petWalker);
                    }
                }
            }
            petWalkers = pet;
        }
        rt.addAttribute("petwalkers", petWalkers);
        return "redirect:/petwalker/list";
    }

    private PetWalkerCriteria createCriteria(SearchPetWalkerDTO searchPetWalker) {
        PetWalkerCriteria petWalkerCriteria = new PetWalkerCriteria();
        if (searchPetWalker != null) {
            if (searchPetWalker.getPetQuantityFrom() != null || searchPetWalker.getPetQuantityTo() != null) {
                IntegerFilter filter = new IntegerFilter();
                if (searchPetWalker.getPetQuantityFrom() != null) {
                    filter.setGreaterThanOrEqual(searchPetWalker.getPetQuantityFrom());
                    petWalkerCriteria.setPetQuantity(filter);
                }
                if (searchPetWalker.getPetQuantityTo() != null) {
                    filter.setLessThanOrEqual(searchPetWalker.getPetQuantityTo());
                    petWalkerCriteria.setPetQuantity(filter);
                }
            }
            if (!StringUtils.isBlank(searchPetWalker.getPetRace())) {
                StringFilter filter = new StringFilter();
                filter.setContains(searchPetWalker.getPetRace());
                petWalkerCriteria.setPetRace(filter);
            }
            if (!StringUtils.isBlank(searchPetWalker.getPetType())) {
                StringFilter filter = new StringFilter();
                filter.setContains(searchPetWalker.getPetType());
                petWalkerCriteria.setPetType(filter);
            }
            if (searchPetWalker.getSalaryFrom() != null || searchPetWalker.getSalaryTo() != null) {
                IntegerFilter filter = new IntegerFilter();
                if (searchPetWalker.getSalaryFrom() != null) {
                    filter.setGreaterThanOrEqual(searchPetWalker.getSalaryFrom());
                    petWalkerCriteria.setSalary(filter);
                }
                if (searchPetWalker.getSalaryTo() != null) {
                    filter.setLessThanOrEqual(searchPetWalker.getSalaryTo());
                    petWalkerCriteria.setSalary(filter);
                }
            }
            if (!StringUtils.isBlank(searchPetWalker.getWorkingZone())) {
                StringFilter filter = new StringFilter();
                filter.setContains(searchPetWalker.getWorkingZone());
                petWalkerCriteria.setWorkingZone(filter);
            }
            if (!StringUtils.isBlank(searchPetWalker.getDescription())) {
                StringFilter filter = new StringFilter();
                filter.setContains(searchPetWalker.getDescription());
                petWalkerCriteria.setDescription(filter);
            }
            if (searchPetWalker.getWorkingHoursFrom() != null) {
                IntegerFilter filter = new IntegerFilter();
                filter.setLessThanOrEqual(searchPetWalker.getHoursFrom());
                petWalkerCriteria.setHoursFrom(filter);
            }
            if (searchPetWalker.getWorkingHoursTo() != null) {
                IntegerFilter filter = new IntegerFilter();
                filter.setGreaterThanOrEqual(searchPetWalker.getHoursTo());
                petWalkerCriteria.setHoursTo(filter);
            }
        }
        return petWalkerCriteria;
    }

    @GetMapping("")
    public PetWalker findById(@RequestParam String id) {
        return petWalkerService.findById(id);
    }

    @GetMapping("/delete")
    public void delete(String id) {
        petWalkerService.delete(id);
    }

    @PostMapping("")
    public void update(String id, PetWalker newPetWalker) {
        petWalkerService.update(id, newPetWalker);

    }
}
