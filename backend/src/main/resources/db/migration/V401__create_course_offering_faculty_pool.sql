-- Admin-curated shortlist of faculty considered for one specific Course Offering -- a further
-- narrowing of FacultyEligibility.eligibleFaculty (Speciality match OR the subject's Eligible
-- Faculty list), which still governs who can ever be added here. The offering's own facultyId and
-- every active CourseOfferingSectionFaculty override for it must always be a pool member --
-- CourseOfferingServiceImpl.updateFacultyPool blocks removing anyone currently relied upon rather
-- than silently orphaning their assignment. Empty means no pool has been built yet.

CREATE TABLE course_offering_faculty_pool (
    course_offering_id BIGINT NOT NULL REFERENCES course_offerings(id) ON DELETE CASCADE,
    faculty_id BIGINT NOT NULL REFERENCES faculty(id) ON DELETE CASCADE,
    PRIMARY KEY (course_offering_id, faculty_id)
);
