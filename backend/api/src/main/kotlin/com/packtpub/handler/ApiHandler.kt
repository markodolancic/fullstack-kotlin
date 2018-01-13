package com.packtpub.handler

import com.packtpub.*
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyToMono
import reactor.core.publisher.Mono
import javax.validation.Validator


class ApiHandler(private val validator: Validator,
                 private val projectService: ProjectService) {

    fun handle(req: ServerRequest) =
        req.bodyToMono<ProjectDTO>()
            .map { project ->
                val violations = validator.validate(project)
                if (violations.isNotEmpty()) {
                    project.fieldErrors = violations.map {
                        FieldErrorDTO(it.propertyPath.toString(), it.message)
                    }
                }
                project
            }
            .flatMap {
                when (it.fieldErrors) {
                    null -> ServerResponse.ok().body(
                        projectService.saveProject(it.toProject()).map { it.toDto() }
                    )
                    else -> ServerResponse.unprocessableEntity().body(Mono.just(it))
                }
            }

    @Suppress("UNUSED_PARAMETER")
    fun getProjects(req: ServerRequest) =
        ServerResponse.ok().body(
            Mono.just(projectService.fetchProjects().map { it.toDto() })
        )

    fun getProject(req: ServerRequest): Mono<ServerResponse> {
        val id = req.pathVariable("id").toLong()
        val projectDTO: ProjectDTO? = projectService.fetchProject(id)?.toDto()
        return if (projectDTO != null) {
            ServerResponse.ok().body(Mono.just(projectDTO))
        } else {
            ServerResponse.notFound().build()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun getOwners(req: ServerRequest): Mono<ServerResponse> =
        ServerResponse.ok().body(Mono.just(projectService.fetchAllOwners()))

    fun getByOwner(req: ServerRequest): Mono<ServerResponse> {
        val name = req.pathVariable("name")
        return ServerResponse.ok().body(Mono.just(projectService.findByOwner(name).map { it.toDto() }))
    }
}